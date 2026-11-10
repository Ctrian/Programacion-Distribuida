# web03 — Helidon SE y PostgreSQL en Docker

Tercera práctica: una aplicación Java inicia su servidor HTTP y consulta una base de datos que se ejecuta en otro proceso, dentro de un contenedor Docker.

## Arquitectura

```text
Cliente -- HTTP :8080 --> Java / Helidon SE
                               |
                               +-- JDBC localhost:5433 --> Docker --> PostgreSQL :5432

Navegador -- HTTP :9090 --> Adminer -- red de Compose --> PostgreSQL :5432
```

La aplicación utiliza la API programática de **Helidon SE** (`WebServer`, rutas y manejadores). Se parece a una aplicación Spring Boot o Quarkus en su forma de arrancar con un `main` y un servidor integrado, aunque sus APIs y mecanismos de configuración son diferentes. Este módulo no utiliza Helidon MP ni las anotaciones REST de `web01` y `web02`.

El servicio llamado `web03` en `docker-compose.yml` ejecuta la imagen de **PostgreSQL**, no la aplicación Java. El otro servicio ejecuta Adminer. No hay un Dockerfile ni una imagen de la aplicación en este módulo: Java se ejecuta en el host Linux.

## Tecnologías y configuración

- Helidon `4.5.6`: servidor HTTP, configuración YAML, soporte JSON y cliente de base de datos.
- PostgreSQL JDBC `42.7.14` y Hikari: acceso JDBC y pool de conexiones.
- Lombok: genera los métodos de `Book` y su builder.
- Shadow: genera un JAR que incorpora las dependencias.
- `src/main/resources/application.yaml`: servidor en `0.0.0.0:8080` y conexión a `jdbc:postgresql://localhost:5433/web03-db`.

Helidon 4 requiere Java 21 o superior. Además, el archivo de ejemplo `org/example/Main.java` usa `IO.println` y un `main` compacto de Java 25; para compilar **todo el código actual del módulo**, utilizar JDK 25. La clase de la aplicación HTTP es `com.programacion.distribuida.Web03Main`.

## Preparar PostgreSQL

Con Docker y Docker Compose disponibles, ejecutar desde `laboratorio/`:

```bash
docker compose -f web03/docker-compose.yml up -d
docker compose -f web03/docker-compose.yml ps
```

Esperar a que PostgreSQL acepte conexiones:

```bash
docker compose -f web03/docker-compose.yml exec web03 pg_isready -U postgres -d web03-db
```

El código consulta las columnas `name` y `title` de `books`. En la práctica ya se creó la tabla y se insertaron datos de prueba mediante la interfaz web, según lo confirmado por el usuario. No hace falta recrear una base ya preparada. Para una base nueva, se puede preparar una tabla mínima de demostración:

```bash
docker compose -f web03/docker-compose.yml exec -T web03 psql -U postgres -d web03-db <<'SQL'
CREATE TABLE IF NOT EXISTS books (
    name TEXT,
    title TEXT
);
INSERT INTO books (name, title) VALUES ('Miguel de Cervantes', 'Don Quijote');
SQL
```

Cada ejecución del `INSERT` añade una fila. Se propone este esquema por las columnas que lee el código; no representa una migración existente del proyecto.

### Datos de prueba y reproducibilidad

Los datos insertados mediante Adminer son registros reales de prueba en PostgreSQL; no son una base simulada dentro de Java. Son suficientes para la demostración local. Lo que todavía no está incluido en el repositorio es el procedimiento automatizado que permite reconstruir esa base en otro equipo.

Sí se puede ejecutar `CREATE TABLE` e `INSERT` desde el `main`, usando `DbClient` antes de arrancar el servidor, pero no es necesario para consultar las tablas que ya existen. Además, un `INSERT` incondicional añadiría los mismos ejemplos en cada arranque. Para este laboratorio puede conservarse el SQL de demostración documentado; como siguiente mejora, se puede guardarlo en un archivo de inicialización o una migración. No se añadieron escrituras automáticas al arranque ni se modificaron los datos existentes.

Adminer queda en `http://localhost:9090`. Elegir PostgreSQL y usar:

| Campo | Valor local |
| --- | --- |
| Servidor | `web03` |
| Usuario | `postgres` |
| Contraseña | `postgres` |
| Base de datos | `web03-db` |

Adminer usa el nombre del servicio y el puerto interno 5432 porque está en la red de Compose. Java utiliza `localhost:5433` porque se ejecuta en el host. Si Java se trasladara a un contenedor de esa red, habría que cambiar la URL a `jdbc:postgresql://web03:5432/web03-db`.

Las credenciales incluidas son valores de práctica local. Compose no fija una versión de la imagen `postgres` ni declara un volumen con nombre para gestionar explícitamente la persistencia. Para reproducir el entorno conviene fijar la versión y definir ese almacenamiento.

## Ejecutar la aplicación

En IntelliJ IDEA, abrir `laboratorio/`, sincronizar Gradle y ejecutar el `main` de `com.programacion.distribuida.Web03Main`. PostgreSQL debe estar disponible y el puerto 8080 libre.

```bash
curl -i --max-time 10 http://localhost:8080/books
```

**Comportamiento actual:** consulta `name` y `title` de la tabla, construye una lista de `Book`, cierra el stream de resultados y la envía como JSON mediante el soporte JSON-B de Helidon. El saludo `ok` de la práctica era una respuesta demostrativa válida; se sustituyó para que el cliente reciba los registros consultados.

Se espera estado `200`, tipo de contenido `application/json` y un array con los datos que existan en la tabla, por ejemplo:

```json
[{"name":"Miguel de Cervantes","title":"Don Quijote"}]
```

El ejemplo no se inserta ni se devuelve como dato fijo en Java; depende del contenido real de PostgreSQL. El orden de las filas y de las propiedades JSON no forma parte del contrato. Una tabla vacía produce `[]`. No se implementaron operaciones de creación, edición ni borrado.

Crear tablas con Adminer confirma que Adminer accede a la base. Para comprobar también Java → PostgreSQL, reconstruir y reiniciar la aplicación y consultar `/books`: si devuelve los registros que se insertaron, quedan comprobadas la consulta JDBC, el mapeo y la serialización HTTP. Acceder a la página de Adminer por sí solo no comprueba esa ruta de Java.

**Prueba de integración realizada tras el cambio:** se compiló el JAR, se inició temporalmente en un puerto local libre y se consultó `/books` contra la base existente. Respondió `200`, `Content-Type: application/json` y un array con **8 libros**, con propiedades `name` y `title`. Se detuvo el proceso temporal al terminar; no se ejecutaron sentencias de escritura ni se modificaron los datos de PostgreSQL. Esto valida el arranque del JAR, la conexión JDBC y la respuesta JSON de esta versión.

## Generar el JAR ejecutable

El manifiesto ya declara la clase existente `com.programacion.distribuida.Web03Main` como `Main-Class`. Shadow hereda esa configuración e incorpora las dependencias; `mergeServiceFiles()` combina los registros de proveedores.

La corrección sustituyó `org.example.web03.Application`, que no existe en los fuentes, por la clase que contiene el `main` del servidor. `java -jar` consulta `META-INF/MANIFEST.MF` para saber qué clase ejecutar: el nombre correcto debe figurar allí y su archivo `.class` debe estar incluido en el JAR. No se cambió el paquete de `Web03Main`. La modificación posterior de `/books` para devolver JSON se describe en la sección de ejecución.

Desde `laboratorio/`, con un JDK 25 completo seleccionado:

```bash
bash gradlew :web03:shadowJar
java -jar web03/build/libs/web03-1.0-SNAPSHOT-all.jar
```

La aplicación necesita PostgreSQL disponible y el puerto 8080 libre también al ejecutarse desde el JAR. El JAR ordinario, sin el sufijo `-all`, no incorpora las dependencias.

`shadowJar` construye el archivo; `java -jar` lo ejecuta. Esta forma es diferente de la distribución de `web02`: allí se ejecuta un script `bin/web02` que utiliza varios JAR de `lib/`; aquí se ejecuta un único JAR con las dependencias incorporadas. No se necesita pasar el nombre de la clase principal en ninguno de esos dos comandos, porque ya quedó configurado al empaquetar. Para detener el proceso de terminal se utiliza `Ctrl+C`.

### Combinación de proveedores

Varias dependencias contienen archivos con el mismo nombre en `META-INF/services/`. Cada archivo registra implementaciones que Java puede descubrir mediante `ServiceLoader`, por ejemplo proveedores de configuración, JSON o acceso a datos.

La estrategia predeterminada `EXCLUDE` descartaba los archivos repetidos antes de que `mergeServiceFiles()` pudiera procesarlos. La tarea ahora permite todos los archivos de `META-INF/services/**` con `DuplicatesStrategy.INCLUDE`, para que el combinador reúna sus proveedores en un solo descriptor. El resto de archivos mantiene `EXCLUDE`.

Por ejemplo, si dos dependencias incluyen el mismo descriptor, una con `ProveedorA` y otra con `ProveedorB`, el archivo final debe registrar ambos. Conservar únicamente el primero puede hacer que Java no encuentre una implementación necesaria durante la ejecución, aunque la compilación termine correctamente.

La configuración aplicada es:

```kotlin
tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
}
```

`INCLUDE` permite entregar todos esos descriptores al combinador; `mergeServiceFiles()` reúne sus listas. No genera un método `main`, un lanzador ni un servidor: permite conservar el descubrimiento de proveedores al reunir las dependencias en el JAR ejecutable.

**Verificación del empaquetado:** `:web03:shadowJar` terminó correctamente con el JDK 25 de IntelliJ, sin las advertencias de proveedores duplicados. Se compararon los descriptores del JAR con los de sus dependencias: conserva los 20 proveedores de los 16 descriptores, sin entradas ZIP duplicadas. Se verificó también `Main-Class: com.programacion.distribuida.Web03Main`. Esa comprobación inicial no incluía el arranque; la prueba posterior de HTTP y PostgreSQL está descrita en la sección de ejecución. En la revisión inicial, Gradle mostró un aviso independiente de características obsoletas para Gradle 10.

Referencia: [Shadow: estrategia de duplicados y combinación de servicios](https://gradleup.com/shadow/configuration/merging/).

## Correcciones y mejoras pendientes

- **Mapeo corregido por el usuario y revisado en los fuentes:** `name` se asigna con `.name(...)` y `title` con `.title(...)`.
- **Respuesta JSON aplicada:** se envía la lista consultada, se retiró el objeto de ejemplo que no se utilizaba y se cierra el stream de resultados mediante `try` con recursos.
- **Base local preparada por el usuario:** tabla y datos de prueba creados desde la interfaz web. La incorporación de SQL o migraciones al repositorio es una mejora de reproducibilidad; no hace falta recrear los datos existentes para la prueba.

Como mejoras de reproducibilidad quedan fijar la versión de la imagen PostgreSQL y declarar almacenamiento persistente explícito. También queda identificar el origen del aviso de características obsoletas antes de migrar a Gradle 10. Estas mejoras son distintas de las correcciones ya aplicadas al JAR.

## Relación con programación distribuida

Aquí aparecen dos comunicaciones de red: cliente–aplicación por HTTP y aplicación–base de datos por el protocolo de PostgreSQL mediante JDBC. Los procesos tienen ciclos de vida independientes: puede funcionar HTTP mientras la base está caída, o fallar una consulta por timeout o credenciales.

Aunque todo esté en una sola máquina, ya se practican separación de procesos, puertos, redes y dependencias remotas. El siguiente paso sería tratar fallos, configurar timeouts, observar el estado de los servicios y separar funcionalidades que deban desplegarse independientemente. Docker facilita el entorno; por sí solo no convierte la aplicación en microservicios.

## Referencias

- [Helidon 4: requisitos y primeros pasos](https://helidon.io/docs/v4/get-started).
- [Helidon SE: configuración](https://helidon.io/docs/v4/se/guides/config).
