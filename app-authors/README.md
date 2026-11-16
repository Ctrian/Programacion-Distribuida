# app-authors — Quarkus y MicroProfile Config

`app-authors` es un módulo del proyecto Gradle `Programacion-Distribuida`. Su funcionalidad actual es una práctica de configuración: expone un recurso REST que lee propiedades mediante MicroProfile Config y muestra los resultados en la consola del servidor.

Aunque su nombre hace referencia a autores, todavía no contiene entidades, repositorios, servicios ni operaciones CRUD de autores. Tiene dependencias para persistencia con PostgreSQL, pero el endpoint actual no consulta una base de datos.

## Tecnologías y compilación

La configuración de [build.gradle.kts](build.gradle.kts) establece:

| Elemento | Versión o función |
| --- | --- |
| Java | 25, tanto para el código fuente como para el bytecode generado. |
| Quarkus | Plugin y BOM `3.40.1.1`. El BOM alinea las versiones de las extensiones. |
| Gradle | Wrapper compartido en la raíz del repositorio, configurado para `9.8.2`. |
| Lombok | Plugin `io.freefair.lombok` `9.8.3`; las clases actuales no usan sus anotaciones. |
| Compilación | Codificación UTF-8 y opción `-parameters` para conservar los nombres de parámetros. |

Las dependencias principales son:

| Dependencia | Propósito |
| --- | --- |
| `quarkus-arc` | Inyección de dependencias con CDI. |
| `quarkus-rest` | Publicación de endpoints con Jakarta REST. |
| `quarkus-rest-jsonb` | Serialización y deserialización JSON con JSON-B. El endpoint actual devuelve una cadena vacía. |
| `quarkus-hibernate-orm` | Persistencia mediante JPA/Hibernate ORM. |
| `quarkus-hibernate-orm-panache` | Facilidades de Panache para entidades y repositorios. |
| `quarkus-jdbc-postgresql` | Driver e integración JDBC para PostgreSQL. |

Las dependencias se resuelven desde Maven Central. Incluir Hibernate y Panache no crea por sí solo el modelo de autores ni sus tablas.

## Estructura del módulo

```text
app-authors/
├── build.gradle.kts
├── README.md
└── src/main/
    ├── java/
    │   ├── com/programacion/distribuida/authors/rest/
    │   │   └── TestConfigRest.java
    │   └── org/example/
    │       └── Main.java
    └── resources/
        ├── application.properties
        └── META-INF/
            └── microprofile-config.properties
```

[TestConfigRest.java](src/main/java/com/programacion/distribuida/authors/rest/TestConfigRest.java) contiene el único recurso REST del módulo. [Main.java](src/main/java/org/example/Main.java) es un ejemplo de consola generado por el IDE: imprime un saludo y los números del 1 al 5. No es el punto de entrada del servidor Quarkus.

## Endpoint `GET /config`

La anotación `@Path("/config")` asigna la ruta a `TestConfigRest`, y `@GET` hace que su método `test()` atienda peticiones HTTP GET.

Cada petición ejecuta esta secuencia:

1. Obtiene el objeto `Config` mediante `ConfigProvider.getConfig()`.
2. Recorre `config.getConfigSources()` e imprime el ordinal y el nombre de cada fuente. No imprime todos sus valores.
3. Recupera `app.name` mediante `config.getValue("app.name", String.class)`.
4. Recupera `app.title` mediante `config.getOptionalValue("app.title", String.class)`.
5. Imprime el nombre leído y los campos `port` y `diAppName`, que recibe por inyección.
6. Imprime el título si existe; en caso contrario, escribe `app.title = <no definido>`.
7. Devuelve `""`.

**La respuesta HTTP tiene un cuerpo vacío. Los datos de configuración se escriben en la consola del servidor**, por lo que no aparecerán como JSON ni como texto informativo en el navegador o en `curl`.

### Lectura programática e inyección

El recurso demuestra dos formas de acceder al mismo sistema de configuración:

```java
// Lectura programática dentro de test().
Config config = ConfigProvider.getConfig();
String appName = config.getValue("app.name", String.class);
Optional<String> appTitle = config.getOptionalValue("app.title", String.class);
```

`getValue()` requiere que la propiedad exista y pueda convertirse al tipo solicitado; si falta `app.name`, la lectura falla. `getOptionalValue()` devuelve un `Optional` vacío cuando falta `app.title`, lo que permite gestionar su ausencia sin tratarla como un error.

```java
// Inyección en los campos del recurso.
@Inject
@ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
Integer port;

@ConfigProperty(name = "app.name")
String diAppName;
```

Quarkus convierte el puerto a `Integer`. El valor `8080` de la anotación es una alternativa si no se encuentra la propiedad; no sobrescribe un puerto definido en otra fuente. `diAppName` es obligatorio porque no tiene valor predeterminado ni tipo `Optional`.

En Quarkus se puede omitir `@Inject` en un campo que ya tiene un calificador como `@ConfigProperty`; por eso `diAppName` también se inyecta. Es una facilidad de Quarkus, documentada en su [referencia de CDI](https://quarkus.io/guides/cdi-reference/#qualified-injected-fields).

## Archivos de configuración

Ambos archivos se cargan como fuentes del mismo sistema de configuración, implementado por SmallRye Config. La API no necesita indicar en qué archivo buscar cada clave.

### `META-INF/microprofile-config.properties`

El archivo [microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties) utiliza la ubicación estándar de MicroProfile Config y contiene:

```properties
app.name=App Authors
```

Esta propiedad propia de la aplicación alimenta tanto la variable local `appName` como el campo inyectado `diAppName`. `app.title` no está definido en los archivos actuales, aunque puede suministrarse desde otra fuente.

### `application.properties`

El archivo [application.properties](src/main/resources/application.properties) contiene actualmente:

```properties
# configuracion quarkus
quarkus.http.port=8080
quarkus.http.host="0.0.0.0"

quarkus.datasource.db-kind=postgresql
#quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/app-authors
#quarkus.datasource.username=postgres
#quarkus.datasource.password=postgres
```

- `quarkus.http.port` establece el puerto HTTP.
- `quarkus.http.host` busca configurar la escucha en todas las interfaces de red. El archivo actual incluye comillas; la forma habitual en `.properties` es `quarkus.http.host=0.0.0.0`, sin comillas. Conviene revisar esta línea si el servidor falla al resolver o enlazar el host.
- `quarkus.datasource.db-kind` selecciona PostgreSQL.
- Las líneas de URL JDBC, usuario y contraseña comienzan con `#`: son ejemplos comentados y no configuran una conexión activa.

También pueden definirse propiedades propias como `app.name` en `application.properties`; no es obligatorio separarlas por archivo.

### Prioridad de las fuentes

Si una propiedad aparece en varias fuentes, prevalece la de mayor ordinal. Las prioridades predeterminadas relevantes son:

| Fuente | Ordinal |
| --- | ---: |
| Propiedades del sistema Java (`-D...`) | 400 |
| Variables de entorno | 300 |
| Archivo `.env` del directorio de trabajo | 295 |
| Archivo externo `config/application.properties` | 260 |
| `application.properties` incluido en el classpath | 250 |
| `META-INF/microprofile-config.properties` incluido en el classpath | 100 |

Así, `application.properties` gana frente a `microprofile-config.properties` si ambos definen `app.name`. Una variable de entorno `APP_NAME` puede sobrescribir a ambos. El listado que imprime el endpoint permite observar las fuentes realmente disponibles, que pueden incluir otras añadidas por Quarkus. Véase la [referencia de configuración](https://quarkus.io/guides/config-reference/#config-sources).

## PostgreSQL y Dev Services

El módulo declara las extensiones de Hibernate, Panache y PostgreSQL, pero aún no implementa acceso a datos de autores. Esto no impide que Quarkus intente preparar un datasource al arrancar.

En desarrollo y pruebas, al no existir una URL explícita, Quarkus puede iniciar PostgreSQL automáticamente mediante Dev Services. Para PostgreSQL necesita un entorno de contenedores compatible disponible, como Docker. Si no está disponible, se debe proporcionar una base de datos externa y configurar la conexión. Esta capacidad se describe en la [guía de Dev Services para bases de datos](https://quarkus.io/guides/databases-dev-services/).

En producción hay que suministrar la conexión real: las tres líneas comentadas del archivo actual no bastan. Se pueden usar las variables `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` y `QUARKUS_DATASOURCE_PASSWORD` para proporcionar esos valores sin escribir credenciales en el repositorio.

## Ejecutar y comprobar el comportamiento

Los siguientes comandos se ejecutan desde la raíz del repositorio `Programacion-Distribuida`, donde se encuentra `gradlew`, con un **JDK 25 completo**. Comprobar que tanto `java -version` como `javac -version` estén disponibles; un entorno que solo permita ejecutar Java no basta para compilar.

Iniciar el modo de desarrollo:

```bash
bash gradlew :app-authors:quarkusDev
```

Con la configuración predeterminada, el puerto es `8080`. Evitar ejecutar simultáneamente otros módulos en ese puerto. Detener el proceso con `Ctrl+C`.

Desde otra terminal, una vez iniciado el servidor:

```bash
curl -i http://localhost:8080/config
```

El cuerpo estará vacío. En la consola del servidor, además del listado de fuentes y los separadores, se espera esta salida si no hay sobrescrituras externas:

```text
app.name = App Authors
app.name = App Authors
Port = 8080
App Name = App Authors
app.title = <no definido>
```

Para comprobar las sobrescrituras y el título opcional, detener el servidor y reiniciarlo con:

```bash
APP_NAME="Autores de prueba" APP_TITLE="Práctica de configuración" bash gradlew :app-authors:quarkusDev
```

Al repetir la petición, la consola debe mostrar esos valores. Son instrucciones de comprobación manual; el módulo actualmente no contiene una carpeta `src/test` ni pruebas automatizadas propias.

## Empaquetar y ejecutar fuera del modo de desarrollo

```bash
bash gradlew :app-authors:quarkusBuild
java -jar app-authors/build/quarkus-app/quarkus-run.jar
```

Para trasladar la aplicación, copiar toda la carpeta `app-authors/build/quarkus-app/`, incluidas sus dependencias. Ejecutar solo el JAR aislado de esa carpeta no basta. La configuración de conexión de producción debe estar disponible al arrancar.

## Detalles pendientes del ejemplo

- `test()` imprime dos veces `app.name`; la salida repetida corresponde al código actual.
- El import de `ConfigProperties` no se utiliza; los campos emplean `ConfigProperty` en singular.
- La salida se realiza con `System.out`, por cada petición, sin una respuesta estructurada ni uso de un logger en el recurso.
- La gestión de autores y las operaciones de persistencia están pendientes de implementación.
