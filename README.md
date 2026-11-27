# Programación Distribuida

Proyecto Gradle con seis módulos. `web01`, `web02` y `web03` se ejecutan de forma independiente. `app-authors` y `app-books` forman el ejemplo integrado de servicios Quarkus con PostgreSQL compartido. `app-customers` introduce Spring Boot y el registro automático en Consul. [Estado de los módulos](#estado-del-proyecto)

## Práctica 1: web01 — REST y despliegue WAR

Aplicación Java que expone un saludo por HTTP mediante Jakarta REST y se empaqueta con Gradle como WAR para desplegarse en un servidor externo.

- Página estática en la raíz de la aplicación.
- Recurso `GET /api/hola/{name}`: devuelve un saludo con la fecha y hora del servidor.
- Separación entre código de aplicación y entorno de ejecución; introducción al modelo cliente-servidor.

Consultar [la documentación de web01](web01/README.md) para compilar, desplegar, comprobar las rutas y entender los requisitos de compatibilidad REST del servidor.

## Práctica 2: web02 — RESTEasy y Undertow embebido

La aplicación inicia su servidor HTTP desde `Web02Main`, sin desplegar un WAR en un servidor externo. RESTEasy implementa Jakarta REST y Undertow atiende las conexiones HTTP. El recurso `GET /api/hola/{name}` devuelve un saludo y la fecha y hora del servidor.

Consultar [la documentación de web02](web02/README.md) para entender la arquitectura, configurar el JDK y ejecutar la aplicación desde IntelliJ, Gradle o el lanzador generado. Esta práctica introduce el control del ciclo de vida de un servicio autónomo como base de programación distribuida.

## Compilar y ejecutar

Ejecutar los comandos desde la raíz del repositorio con un **JDK 25 completo**. Comprobar que `java -version` y `javac -version` correspondan al mismo JDK. El wrapper usa Gradle `9.8.2`.

```bash
bash gradlew :web01:war
```

El resultado es `web01/build/libs/web01-1.0-SNAPSHOT.war`. El WAR no incorpora una implementación Jakarta REST; el servidor debe proporcionarla. Los fuentes y el wrapper se versionan; los archivos generados en `build/` no se incluyen.

Para iniciar `web02` mediante Gradle:

```bash
bash gradlew :web02:run
```

O generar su distribución y ejecutar el lanzador con sus dependencias:

```bash
bash gradlew :web02:installDist
./web02/build/install/web02/bin/web02
```

Comprobar desde otra terminal:

```bash
curl -i http://localhost:8080/api/hola/Ana
```

Se espera estado `200` y el texto `hola Ana - ...`. `web01`, `web02`, `web03` y `app-books` usan el puerto 8080 en las configuraciones de ejemplo. Ejecutarlos por separado o asignarles puertos distintos. Detener `web02` con `Ctrl+C` si se arrancó desde terminal.

## Práctica 3: web03 — Helidon y PostgreSQL

API de libros con Helidon SE, respuesta JSON y acceso JDBC a PostgreSQL.
Su Compose inicia PostgreSQL para `web03-db` en el puerto 5433 y Adminer en 9090. Para una base nueva, la guía incluye el SQL de demostración.

Consultar [la documentación de web03](web03/README.md).


## Práctica 4: app-authors — Quarkus

Servicio con Quarkus, CDI, Jakarta REST, JSON-B y persistencia con Panache. Expone `GET /authors`, `GET /authors/{id}` y `GET /authors/find/{isbn}` para consultar autores. Conserva `GET /config` como ejemplo de MicroProfile Config: devuelve un cuerpo vacío y escribe los valores en consola.

`app-authors` aplica con Flyway las migraciones de la base compartida al arrancar. También intenta registrar su instancia en Consul, que se ejecuta por separado y es opcional para usar la API. [Migraciones](app-authors/README.md#migraciones-de-la-base-compartida) · [Registro en Consul](app-authors/README.md#registro-en-consul)

## Práctica 5: app-books — libros e inventario con Quarkus

API de consulta de libros e inventario con Java 25, Jakarta REST, JSON-B y Panache. Expone `GET /books` y `GET /books/{isbn}`. El detalle obtiene los autores por HTTP consultando `GET /authors/find/{isbn}` en `app-authors`. El inventario comparte el ISBN del libro como clave primaria y foránea mediante `@MapsId`. [Guía de app-books](app-books/README.md)

`app-books` intenta registrar su propia instancia en Consul. El cliente de autores sigue usando la URL configurada en `app.authors.url`. [Registro de libros en Consul](app-books/README.md#registro-en-consul)

### Arrancar autores y libros

Desde la raíz del repositorio, con JDK 25 y Docker Compose, iniciar PostgreSQL y comprobar que acepte conexiones.

```bash
docker compose -f app-authors/docker-compose.yml up -d
docker compose -f app-authors/docker-compose.yml exec -T authors pg_isready -U postgres -d books-db
```

Con una base nueva o una base que ya tenga historial de Flyway, iniciar `app-authors` con el comando normal. Si la base tiene tablas creadas manualmente y carece de historial, primero verificar su estado y seguir el flujo de baseline. [Base inicializada manualmente](app-authors/README.md#base-inicializada-manualmente)

```bash
bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
```

Una vez que `app-authors` esté listo, iniciar `app-books` en otra terminal. `-Ddebug=false` evita que ambos procesos intenten usar el puerto de depuración 5005.

```bash
bash gradlew :app-books:quarkusDev -Ddebug=false --console=plain
```

| Proceso | Puerto en el equipo |
| --- | ---: |
| API de autores | 8070 |
| API de libros | 8080 |
| Servidor de clientes | 8090 |
| PostgreSQL compartido (`books-db`) | 5434 |
| Adminer de la práctica | 9091 |
| Consul, si se ejecuta por separado | 8500 |

Con `quarkus.flyway.migrate-at-start=true` y `quarkus.flyway.baseline-on-migrate=false`, Flyway crea las tablas y carga los datos de ejemplo en una base vacía. En una base con historial, aplica las versiones pendientes. Por ejemplo, si ya figura `1.0.3` en el historial y se añade una migración `1.0.4`, el arranque normal aplica `1.0.4` una sola vez. El baseline automático se activa para el primer arranque de una base preparada manualmente y sin historial, con la versión de partida indicada explícitamente. [Flujo de Flyway](app-authors/README.md#migraciones-de-la-base-compartida) · [SQL de autores y libros](docs/sql-autores-libros.md) · [SQL de inventario](docs/sql-inventario-libros.md)

> **Segunda instancia de `app-authors` en IntelliJ IDEA**
>
> 1. Abrir **Run → Edit Configurations…**, duplicar la configuración Gradle de `:app-authors:quarkusDev` y nombrar la copia `app-authors 8071`.
> 2. Pegar esta línea en el campo **Run** o **Tasks and Arguments**.
>
>    ```text
>    :app-authors:quarkusDev --jvm-args=-Dquarkus.http.port=8071 -Ddebug=false
>    ```
>
> 3. Guardar con **Apply → OK**, iniciar la copia con **Run** y comprobar que responde en `http://localhost:8071/authors` mientras la primera instancia sigue en `8070`.
>
> `--jvm-args` pasa el puerto HTTP a la JVM de Quarkus. `-Ddebug=false` evita que la segunda instancia intente usar el puerto de depuración predeterminado `5005`. Para depurar la segunda instancia, usar `-Ddebug=5006` en su lugar.

## Práctica 6: app-customers — Spring Boot y Consul

Módulo Spring Boot con Spring MVC y Actuator. Escucha en `8090` y, en el arranque normal, registra el servicio `app-customers` en el agente Consul de `localhost:8500`. Por ahora se puede comprobar el estado del servidor con `GET /actuator/health`. [Guía de app-customers](app-customers/README.md)

## Estado del proyecto

| Módulo | Estado actual |
| --- | --- |
| [web01](web01/README.md) | WAR con página estática y saludo Jakarta REST. El endpoint requiere un servidor externo que proporcione una implementación compatible. |
| [web02](web02/README.md) | Servicio autónomo con RESTEasy y Undertow. Expone el saludo en `GET /api/hola/{name}` y se ejecuta con Gradle o el lanzador de la distribución. |
| [web03](web03/README.md) | API Helidon SE de solo lectura. `GET /books` devuelve JSON desde su propia base `web03-db`, cuya tabla de demostración se prepara manualmente. |
| [app-authors](app-authors/README.md) | API de consulta de autores. Administra el esquema compartido con Flyway hasta `1.0.3` e intenta registrar la instancia en Consul. |
| [app-books](app-books/README.md) | API de consulta de libros e inventario. Usa `books-db`, consulta autores mediante una URL HTTP configurada e intenta registrar su propia instancia en Consul. |
| [app-customers](app-customers/README.md) | Módulo Spring Boot con Actuator y registro automático en Consul. La API de clientes está pendiente. |

En una prueba previa sobre PostgreSQL 18.6, las tres migraciones reprodujeron el esquema y los datos de los scripts manuales en una base temporal. También se verificó el arranque de `app-authors` sobre una base existente con baseline `1.0.3` y la respuesta HTTP 200 de `GET /authors`.

## Organización del repositorio

La rama única es `main`. Integra los seis módulos y conserva el historial de las antiguas ramas `main` y `master` mediante un commit de fusión.

La documentación original de `master` conserva el contexto de publicación de las primeras prácticas. Para la estructura y los comandos actuales, usar este README y los de cada módulo. [Archivo histórico](docs/historial/README-master.md)
