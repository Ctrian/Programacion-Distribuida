# app-authors — Quarkus y MicroProfile Config

`app-authors` es un módulo del proyecto Gradle `Programacion-Distribuida`. Expone una API de consulta de autores con Jakarta REST y Panache, además de un endpoint de práctica que lee propiedades mediante MicroProfile Config y muestra los resultados en la consola del servidor.

Contiene las entidades `Author`, `BookAuthor` y `BookAuthorId`, el repositorio `AuthorRepository` y el recurso `AuthorRest`. La API ofrece consultas de lectura. El módulo aplica las migraciones de Flyway sobre la base PostgreSQL que comparte con `app-books`.

## Puertos y arranque paso a paso

| Proceso | Puerto en el equipo | Estado |
| --- | ---: | --- |
| API `app-authors` | 8070 | Implementada con Quarkus. |
| API `app-books` | 8080 | Consultas de libros e inventario con Quarkus. |
| PostgreSQL compartido | 5434 | Docker lo redirige al 5432 del contenedor. |
| Adminer | 9091 | Docker lo redirige al 8080 del contenedor; no ocupa el 8080 del equipo. |

Ejecutar desde la raíz `Programacion-Distribuida`:

1. Seleccionar el JDK 25 completo instalado en este equipo:

   ```bash
   export JAVA_HOME="$HOME/.jdks/jbr-25.0.4.1"
   export PATH="$JAVA_HOME/bin:$PATH"
   java -version
   javac -version
   ```

   En otro equipo, usar la ruta de su JDK 25. IntelliJ puede tener un JDK configurado aunque la terminal use un entorno sin compilador.

2. Iniciar y comprobar PostgreSQL y Adminer:

   ```bash
   docker compose -f app-authors/docker-compose.yml up -d
   docker compose -f app-authors/docker-compose.yml ps
   docker compose -f app-authors/docker-compose.yml exec -T authors pg_isready -U postgres -d books-db
   ```

3. Cuando PostgreSQL acepte conexiones, iniciar el servicio de autores y mantener esta terminal abierta:

   Si la base ya fue preparada con los scripts manuales, consultar cómo incorporarla a Flyway antes de arrancar. [Base inicializada manualmente](#base-inicializada-manualmente)

   ```bash
   bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
   ```

   En una base nueva, Flyway crea `authors`, `books`, `books_authors` e `inventory` y carga los datos de ejemplo. [Migraciones de la base compartida](#migraciones-de-la-base-compartida)

4. Desde otra terminal, comprobar las consultas:

   ```bash
   curl -i http://localhost:8070/authors
   curl -i http://localhost:8070/authors/1
   curl -i http://localhost:8070/authors/find/978-0136083238
   curl -i http://localhost:8070/config
   ```

   Con la carga de ejemplo se esperan cinco autores en la primera consulta y los autores con IDs 1, 2, 4 y 5 en la consulta por ISBN. `/config` devuelve un cuerpo vacío y escribe sus resultados en la consola. Un ID inexistente devuelve 404.

5. Para detener la API, pulsar `Ctrl+C`. Para detener los contenedores conservándolos:

   ```bash
   docker compose -f app-authors/docker-compose.yml stop
   ```

Para entrar a Adminer, abrir `http://localhost:9091`, seleccionar PostgreSQL y usar servidor `authors`, usuario `postgres`, contraseña `postgres` y base `books-db`. Dentro de Docker se usa el nombre del servicio, no `localhost:5434`. El Compose actual no declara un volumen con nombre: conservar los contenedores y hacer una copia antes de eliminarlos o recrearlos si los datos importan.

Cuando `app-authors` esté listo, abrir otra terminal y ejecutar `bash gradlew :app-books:quarkusDev -Ddebug=false --console=plain`. La opción `-Ddebug=false` evita el conflicto del puerto de depuración 5005. `app-books` usa `GET /authors/find/{isbn}` para consultar autores por HTTP. [README de app-books](../app-books/README.md)

## API y persistencia de autores

| Endpoint | Comportamiento |
| --- | --- |
| `GET /authors` | Lista los autores como JSON. |
| `GET /authors/{id}` | Busca por ID (`Long`); devuelve JSON o 404. |
| `GET /authors/find/{isbn}` | Consulta los autores asociados al ISBN; devuelve una lista JSON. |
| `GET /config` | Demuestra la lectura de configuración y escribe en consola. |

`Author` mapea `authors`; `BookAuthor` mapea `books_authors` y contiene una relación `@ManyToOne` hacia `Author`. Su clave compuesta contiene `booksIsbn` y `authorId`, mapeados a `books_isbn` y `authors_id`, e implementa igualdad y serialización. Los IDs de autores usan `Long`, coherente con `bigint` del SQL.

`AuthorRepository` usa `PanacheRepositoryBase<Author, Long>` con alcance de aplicación y transacciones. Su consulta por ISBN es `select o.author from BookAuthor o where o.id.booksIsbn=?1`: usa nombres de entidades y atributos Java. `AuthorRest` recibe el repositorio por constructor.

## Tecnologías y compilación

La configuración de [build.gradle.kts](build.gradle.kts) establece:

| Elemento | Versión o función |
| --- | --- |
| Java | 25, tanto para el código fuente como para el bytecode generado. |
| Quarkus | Plugin y BOM `3.40.1.1`. El BOM alinea las versiones de las extensiones. |
| Gradle | Wrapper compartido en la raíz del repositorio, configurado para `9.8.2`. |
| Lombok | Plugin `io.freefair.lombok` `9.8.3`; genera getters, setters, igualdad y métodos `toString()` en las entidades. |
| Compilación | Codificación UTF-8 y opción `-parameters` para conservar los nombres de parámetros. |

Las dependencias principales son:

| Dependencia | Propósito |
| --- | --- |
| `quarkus-arc` | Inyección de dependencias con CDI. |
| `quarkus-rest` | Publicación de endpoints con Jakarta REST. |
| `quarkus-rest-jsonb` | Serialización JSON de las consultas de autores. El endpoint `/config` devuelve una cadena vacía. |
| `quarkus-hibernate-orm` | Persistencia mediante JPA/Hibernate ORM. |
| `quarkus-hibernate-orm-panache` | Facilidades de Panache para entidades y repositorios. |
| `quarkus-jdbc-postgresql` | Driver e integración JDBC para PostgreSQL. |
| `quarkus-flyway` | Integración de Flyway con el arranque de Quarkus. |
| `flyway-database-postgresql` | Soporte de PostgreSQL para Flyway. |

Las dependencias se resuelven desde Maven Central. Hibernate mapea las entidades y Flyway administra las tablas mediante los SQL versionados.

## Estructura del módulo

```text
app-authors/
├── build.gradle.kts
├── README.md
├── docker-compose.yml
├── sql/init.sql
├── docs/application-helidon-reference.yaml
└── src/main/
    ├── java/
    │   ├── com/programacion/distribuida/authors/
    │   │   ├── db/{Author,BookAuthor,BookAuthorId}.java
    │   │   ├── repo/AuthorRepository.java
    │   │   └── rest/{AuthorRest,TestConfigRest}.java
    │   └── org/example/
    │       └── Main.java
    └── resources/
        ├── application.properties
        ├── META-INF/microprofile-config.properties
        └── db/migration/
            ├── V1.0.1__crear-tablas-autores.sql
            ├── V1.0.2__crear-tabla-libros.sql
            └── V1.0.3__crear-tablas-inventory.sql
```

[TestConfigRest.java](src/main/java/com/programacion/distribuida/authors/rest/TestConfigRest.java) contiene el recurso de prueba de configuración; `AuthorRest` contiene las consultas de autores. [Main.java](src/main/java/org/example/Main.java) es un ejemplo de consola generado por el IDE: imprime un saludo y los números del 1 al 5. No es el punto de entrada del servidor Quarkus.

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
@ConfigProperty(name = "quarkus.http.port", defaultValue = "8070")
Integer port;

@ConfigProperty(name = "app.name")
String diAppName;
```

Quarkus convierte el puerto a `Integer`. El valor `8070` de la anotación es una alternativa si no se encuentra la propiedad; no sobrescribe un puerto definido en otra fuente. `diAppName` es obligatorio porque no tiene valor predeterminado ni tipo `Optional`.

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
quarkus.http.port=8070
quarkus.http.host=0.0.0.0

quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=${AUTHORS_DB_URL:jdbc:postgresql://localhost:5434/books-db}
quarkus.datasource.username=${AUTHORS_DB_USERNAME:postgres}
quarkus.datasource.password=${AUTHORS_DB_PASSWORD:postgres}
quarkus.datasource.devservices.enabled=false
quarkus.hibernate-orm.schema-management.strategy=none

quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true
```

`quarkus.http.port` fija el puerto 8070 y `quarkus.http.host` permite escuchar en todas las interfaces. Las expresiones `${VARIABLE:valor}` permiten sobrescribir la conexión mediante variables de entorno; por defecto apuntan al PostgreSQL del Compose, en `localhost:5434/books-db`. Las credenciales predeterminadas son solo para la práctica local.

Dev Services está desactivado porque la base se levanta explícitamente con Docker Compose. `schema-management.strategy=none` impide que Hibernate cree o borre tablas.

El archivo `docs/application-helidon-reference.yaml` conserva el antiguo `application.yaml` como referencia heredada con claves `server.*` y `db.*`. No configura Quarkus: el módulo no incluye `quarkus-config-yaml` y, aun con esa extensión, las claves de servidor deben ser `quarkus.http.*`. La configuración efectiva se concentra en `application.properties`. Véase la [guía de YAML de Quarkus](https://quarkus.io/guides/config-yaml/).

También pueden definirse propiedades propias como `app.name` en `application.properties`.

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

## PostgreSQL y variables de entorno

Docker Compose levanta PostgreSQL. Al inicializar un directorio de datos vacío, el contenedor crea la base `books-db`. Al arrancar, `app-authors` conecta a esa base y Flyway valida y aplica las migraciones pendientes.

Para usar una base diferente, definir `AUTHORS_DB_URL`, `AUTHORS_DB_USERNAME` y `AUTHORS_DB_PASSWORD`. Las variables estándar `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` y `QUARKUS_DATASOURCE_PASSWORD` también permiten sobrescribir la conexión. El usuario de PostgreSQL necesita permisos para aplicar las migraciones pendientes y registrar su ejecución.

## Migraciones de la base compartida

Flyway busca los archivos `V<versión>__<descripción>.sql` en `src/main/resources/db/migration/`. La secuencia de versiones es común a autores, libros e inventario.

| Versión | Cambio |
| --- | --- |
| [`1.0.1`](src/main/resources/db/migration/V1.0.1__crear-tablas-autores.sql) | Crea `authors`, carga cinco autores y ajusta la secuencia de IDs. |
| [`1.0.2`](src/main/resources/db/migration/V1.0.2__crear-tabla-libros.sql) | Crea `books` y `books_authors`, carga cinco libros y doce relaciones. |
| [`1.0.3`](src/main/resources/db/migration/V1.0.3__crear-tablas-inventory.sql) | Crea `inventory` con sus restricciones y carga el inventario de *Effective Java*. |

`migrate-at-start=true` aplica las versiones pendientes al ejecutar `quarkusDev` o al arrancar el JAR empaquetado. `quarkusBuild` prepara el paquete. En este flujo se omiten los antiguos scripts de inicialización manual. Flyway registra las migraciones aplicadas en `public.flyway_schema_history` de cada base, con el nombre del archivo, su *checksum* y el resultado. Consultar la tabla desde Adminer o `psql`:

```sql
SELECT version, script, checksum, installed_on, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

Para cambiar una tabla, crear un archivo con una versión posterior. Por ejemplo, `V1.0.4__agregar-email-a-autores.sql` podría contener este cambio:

```sql
ALTER TABLE public.authors ADD COLUMN email varchar(255);
```

En una base al día hasta `1.0.3`, el próximo arranque solo ejecutaría `1.0.4`. Una base nueva ejecutaría las cuatro versiones en orden. Si se modifica el contenido de un archivo ya aplicado y cambia su *checksum*, Flyway detiene el arranque con un error de validación. Restaurar el contenido aplicado y trasladar el cambio al archivo nuevo. Las migraciones pueden incluir cambios de esquema y datos de referencia explícitos en SQL. Los datos agregados durante el uso de la aplicación permanecen en su propia base.

### Base inicializada manualmente

Con `baseline-on-migrate=true`, una base con tablas y sin historial se marca con una versión de partida. Flyway omite las migraciones hasta esa versión inclusive. El valor predeterminado es `1`, así que usarlo con las tablas de esta práctica ya creadas haría que Flyway intentara ejecutar `1.0.1` y chocara con `authors`.

Antes de adoptar esa base, comprobar qué migraciones representan su esquema y su carga inicial. Si ya corresponden a las tres versiones, usar `1.0.3` como punto de partida. Si solo corresponden a autores, libros y relaciones y falta inventario, usar `1.0.2` para que Flyway aplique `1.0.3`. Si el estado es parcial, revisar las diferencias antes de fijar el punto de partida. [Referencias SQL de autores y libros](../docs/sql-autores-libros.md) · [Inventario](../docs/sql-inventario-libros.md)

Ejemplo de primer arranque para una base equivalente a `1.0.3` sin historial:

```bash
QUARKUS_FLYWAY_BASELINE_VERSION=1.0.3 bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
```

Para los siguientes arranques, Flyway usa el historial que ya existe en la base. [Guía de Flyway en Quarkus](https://quarkus.io/guides/flyway/) · [Configuración de `baselineOnMigrate`](https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting)

## Ejecutar y comprobar el comportamiento

Los siguientes comandos se ejecutan desde la raíz del repositorio `Programacion-Distribuida`, donde se encuentra `gradlew`, con un **JDK 25 completo**. Comprobar que tanto `java -version` como `javac -version` estén disponibles; un entorno que solo permita ejecutar Java no basta para compilar.

Iniciar el modo de desarrollo:

```bash
bash gradlew :app-authors:quarkusDev -Ddebug=false
```

Con la configuración predeterminada, el puerto es `8070`. Evitar ejecutar simultáneamente otros módulos en ese puerto. Detener el proceso con `Ctrl+C`.

Desde otra terminal, una vez iniciado el servidor:

```bash
curl -i http://localhost:8070/config
```

El cuerpo estará vacío. En la consola del servidor, además del listado de fuentes y los separadores, se espera esta salida si no hay sobrescrituras externas:

```text
app.name = App Authors
app.name = App Authors
Port = 8070
App Name = App Authors
app.title = <no definido>
```

Para comprobar las sobrescrituras y el título opcional, detener el servidor y reiniciarlo con:

```bash
APP_NAME="Autores de prueba" APP_TITLE="Práctica de configuración" bash gradlew :app-authors:quarkusDev -Ddebug=false
```

Al repetir la petición, la consola debe mostrar esos valores. Son instrucciones de comprobación manual; el módulo actualmente no contiene una carpeta `src/test` ni pruebas automatizadas propias.

## Empaquetar y ejecutar fuera del modo de desarrollo

```bash
bash gradlew :app-authors:quarkusBuild
java -jar app-authors/build/quarkus-app/quarkus-run.jar
```

Para trasladar la aplicación, copiar toda la carpeta `app-authors/build/quarkus-app/`, incluidas sus dependencias. Proporcionar la conexión a PostgreSQL al ejecutar el JAR. Flyway aplica en ese arranque las migraciones pendientes de la base configurada.

## Detalles pendientes del ejemplo

- `test()` imprime dos veces `app.name`; la salida repetida corresponde al código actual.
- La salida se realiza con `System.out`, por cada petición, sin una respuesta estructurada ni uso de un logger en el recurso.
- Las operaciones de escritura de autores están pendientes de implementación.
