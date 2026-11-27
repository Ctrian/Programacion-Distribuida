# app-books — API de libros e inventario con Quarkus

`app-books` expone consultas de libros mediante Jakarta REST, JSON-B y Hibernate ORM con Panache. Usa Java 25, Quarkus `3.40.1.1`, Lombok `9.8.3` y el wrapper Gradle `9.8.2` compartido con los demás módulos del repositorio.

La API escucha en **8080**. Ambos servicios acceden a la base PostgreSQL `books-db`, publicada en **5434** por el Compose de `app-authors`. `app-books` lee los libros y el inventario desde PostgreSQL y consulta los autores por HTTP en `app-authors`, que escucha en **8070**.

## Estructura y dependencias

```text
app-books/
├── build.gradle.kts
├── README.md
├── sql/init-inventory.sql
└── src/main/
    ├── java/
    │   ├── com/programacion/distribuida/books/
    │   │   ├── BooksLifeCycle.java
    │   │   ├── clients/AuthorRestClient.java
    │   │   ├── config/RestClientConfig.java
    │   │   ├── db/Book.java
    │   │   ├── db/Inventory.java
    │   │   ├── dtos/{AuthorDto,BookDto}.java
    │   │   ├── repo/BookRepository.java
    │   │   ├── rest/BookRest.java
    │   │   └── service/{BookService.java,impl/BookServiceImpl.java}
    │   └── org/example/Main.java
    └── resources/
        ├── application.properties
        └── META-INF/microprofile-config.properties
```

[build.gradle.kts](build.gradle.kts) declara el plugin Quarkus y su BOM, CDI (`quarkus-arc`), Jakarta REST (`quarkus-rest`), JSON-B (`quarkus-rest-jsonb`), Hibernate ORM, Panache y el driver JDBC de PostgreSQL. `quarkus-rest-client` y `quarkus-rest-client-jsonb` aportan el cliente HTTP y el mapeo JSON de los autores. La compilación usa UTF-8 y `-parameters`. Lombok genera getters, setters y métodos `toString()` de las entidades.

La clase `org.example.Main` es un ejemplo de consola del IDE; no inicia la API. Quarkus arranca mediante la tarea `quarkusDev` o el JAR empaquetado.

## Modelo y endpoints

| Clase | Función |
| --- | --- |
| [Book](src/main/java/com/programacion/distribuida/books/db/Book.java) | Mapea `books`: ISBN como ID `String`, título, precio `BigDecimal` y relación uno a uno con inventario. |
| [Inventory](src/main/java/com/programacion/distribuida/books/db/Inventory.java) | Mapea `inventory`: ISBN, unidades vendidas (`sold`) y suministradas (`supplied`). |
| [BookRepository](src/main/java/com/programacion/distribuida/books/repo/BookRepository.java) | Repositorio CDI transaccional que implementa `PanacheRepositoryBase<Book, String>`. |
| [AuthorRestClient](src/main/java/com/programacion/distribuida/books/clients/AuthorRestClient.java) | Cliente MicroProfile REST para `GET /authors/find/{isbn}`. |
| [BookServiceImpl](src/main/java/com/programacion/distribuida/books/service/impl/BookServiceImpl.java) | Consulta el repositorio, construye el `BookDto` y agrega los autores al detalle del libro. |
| [BookRest](src/main/java/com/programacion/distribuida/books/rest/BookRest.java) | Delega las consultas al servicio y devuelve los DTO como JSON. |

`Inventory.bookIsbn` mapea `book_isbn`, que es clave primaria y referencia al libro. `@MapsId` hace que la asociación `Inventory.book` comparta ese identificador. En `Book`, `@OneToOne(mappedBy = "book")` declara el lado inverso de la asociación.

| Endpoint | Resultado |
| --- | --- |
| `GET /books` | Lista JSON de libros con sus cantidades de inventario, o `[]` si la tabla está vacía. |
| `GET /books/{isbn}` | Devuelve 200 con el libro, las cantidades de inventario y `authors`. Devuelve 404 para un ISBN ausente si la consulta a autores se completa. |

Los dos endpoints devuelven `inventorySold` (unidades vendidas) e `inventorySupplied` (unidades suministradas) en el JSON del libro. Cuando falta la fila de `inventory`, los dos campos valen `0`. Las existencias disponibles se calculan restando `inventorySold` de `inventorySupplied`. Con los datos de ejemplo, *Effective Java* tiene 50 unidades suministradas, 10 vendidas y 40 disponibles.

En el código actual, el listado consulta autores por cada libro y descarta esas respuestas, por lo que `authors` se rellena únicamente en el detalle. El detalle llama a `app-authors` incluso antes de comprobar si el ISBN existe. Si falla la llamada HTTP, la consulta de libros que la hizo también puede fallar.

Para distinguir un libro sin inventario registrado de uno con cantidades en cero, consulta la fila por `book_isbn` en PostgreSQL. Usa las [consultas SQL de inventario](../docs/sql-inventario-libros.md#consultas-para-comprobar-la-carga). La API de libros ofrece consultas de lectura y la columna `version` existe en SQL sin un mapeo `@Version` en Java.

## Configuración

[application.properties](src/main/resources/application.properties) establece:

```properties
quarkus.http.port=8080
quarkus.http.host=0.0.0.0

quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5434/books-db
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.devservices.enabled=false

quarkus.hibernate-orm.schema-management.strategy=none
```

Dev Services está desactivado porque PostgreSQL se inicia explícitamente con Docker Compose. `app-books` usa el esquema que Flyway prepara al arrancar `app-authors`. En desarrollo, Hibernate puede informar diferencias del esquema mediante su comprobación en segundo plano. [Migraciones de la base compartida](../app-authors/README.md#migraciones-de-la-base-compartida)

Las credenciales son valores de práctica local. Para otra conexión se pueden usar `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` y `QUARKUS_DATASOURCE_PASSWORD`; esas variables tienen prioridad sobre el archivo.

El cliente registrado con `@RegisterRestClient(configKey = "AuthorRestClient")` obtiene su URL base de estas propiedades de [microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties):

```properties
app.authors.url=http://localhost:8070
AuthorRestClient/mp-rest/url=${app.authors.url}
```

Para usar otra instancia de autores, definir `APP_AUTHORS_URL` en el entorno de `app-books`. El valor es la URL base del servicio. `AuthorRestClient` añade la ruta `/authors/find/{isbn}`.

El cliente REST de autores usa HTTP directo con la URL configurada. El módulo incluye `quarkus-smallrye-stork` como dependencia para descubrimiento y balanceo. [Registro de autores en Consul](../app-authors/README.md#registro-en-consul)

## Registro en Consul

Durante el arranque, `BooksLifeCycle` intenta registrar el servicio `app-books` con el identificador `app-books-127.0.0.1:8080` y anuncia la dirección `127.0.0.1:8080` con la configuración predeterminada. Al detener Quarkus, intenta retirar la instancia si se registró correctamente. El registro está pensado para la práctica local, donde el consumidor puede acceder al servicio por esa dirección. [Código del ciclo de vida](src/main/java/com/programacion/distribuida/books/BooksLifeCycle.java)

El agente Consul se ejecuta por separado del Compose de PostgreSQL y Adminer. La configuración apunta a `127.0.0.1:8500` y admite las variables `CONSUL_HOST` y `CONSUL_PORT`. Si el agente no está disponible, el ciclo de vida de libros escribe el fallo en consola y la aplicación sigue arrancando. El registro requiere un puerto HTTP fijo. [Configuración de MicroProfile](src/main/resources/META-INF/microprofile-config.properties)

Con el agente disponible y `app-books` iniciado, listar sus registros y buscar `app-books`.

```bash
curl -fsS http://localhost:8500/v1/agent/services
```

## Arranque paso a paso

Ejecutar los comandos desde la raíz del repositorio `Programacion-Distribuida`.

1. Seleccionar un JDK 25 completo y comprobar las versiones de la terminal.

   ```bash
   java -version
   javac -version
   ```

   Si hace falta seleccionar otro JDK, ajustar `JAVA_HOME` y `PATH`.

2. Levantar PostgreSQL y Adminer:

   ```bash
   docker compose -f app-authors/docker-compose.yml up -d
   docker compose -f app-authors/docker-compose.yml ps
   docker compose -f app-authors/docker-compose.yml exec -T authors pg_isready -U postgres -d books-db
   ```

   Esperar a que PostgreSQL acepte conexiones. Se usa `5434` en el equipo para evitar el `5433` de la práctica `web03`. Adminer se publica en `http://localhost:9091`; su puerto interno 8080 no ocupa el puerto HTTP de libros.

3. Iniciar autores para que Flyway prepare la base compartida y para que el cliente HTTP tenga un servicio disponible.

   En una base vacía, Flyway aplica todas las migraciones; con historial ya registrado, aplica las pendientes. En ambos casos se usa el comando normal y el baseline automático queda desactivado. Para incorporar una base preparada a mano y sin historial, configurar el baseline en el primer arranque de `app-authors`. [Base inicializada manualmente](../app-authors/README.md#base-inicializada-manualmente)

   ```bash
   bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
   ```

4. Cuando `app-authors` esté listo, abrir otra terminal con el mismo JDK e iniciar libros.

   ```bash
   bash gradlew :app-books:quarkusDev -Ddebug=false --console=plain
   ```

   `-Ddebug=false` desactiva el depurador para evitar que ambos procesos ocupen el puerto 5005 predeterminado. Los puertos HTTP siguen siendo 8080 y 8070. Véase la [guía de Quarkus con Gradle](https://quarkus.io/guides/gradle-tooling/).

5. Comprobar desde otra terminal:

   ```bash
   curl -i http://localhost:8080/books
   curl -i http://localhost:8080/books/978-0134686097
   curl -i http://localhost:8080/books/isbn-inexistente
   curl -i http://localhost:8070/authors/find/978-0134686097
   ```

   Con la carga de ejemplo, el listado contiene cinco libros. Para `978-0134686097`, el detalle devuelve `inventorySold: 10`, `inventorySupplied: 50` y los autores con IDs `1` y `5`. La consulta directa en `8070` devuelve esos mismos autores. Con `app-authors` disponible, el ISBN inexistente devuelve 404.

6. Detener cada API con `Ctrl+C`. Detener los contenedores cuando ninguno de los dos servicios necesite la base:

   ```bash
   docker compose -f app-authors/docker-compose.yml stop
   ```

El Compose actual no declara un volumen con nombre. Conservar los contenedores y respaldar los datos antes de eliminarlos o recrearlos.

## Compilar y empaquetar

```bash
bash gradlew :app-authors:quarkusBuild :app-books:quarkusBuild
```

Con PostgreSQL disponible, ejecutar primero el JAR de autores para aplicar las migraciones pendientes:

```bash
java -jar app-authors/build/quarkus-app/quarkus-run.jar
```

Cuando autores esté listo, ejecutar el JAR de libros en otra terminal:

```bash
java -jar app-books/build/quarkus-app/quarkus-run.jar
```

Para distribuir cada servicio, copiar su carpeta `build/quarkus-app/` completa con el JAR y sus dependencias. Configurar la conexión a PostgreSQL en ambos servicios y la URL de autores en `app-books` según el entorno de despliegue.
