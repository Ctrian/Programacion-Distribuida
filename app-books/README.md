# app-books — API de libros e inventario con Quarkus

`app-books` expone consultas de libros mediante Jakarta REST, JSON-B y Hibernate ORM con Panache. Usa Java 25, Quarkus `3.40.1.1`, Lombok `9.8.3` y el wrapper Gradle `9.8.2` compartido con los demás módulos del repositorio.

La API escucha en **8080**. `app-authors` escucha en **8070**. En esta práctica ambos servicios acceden a la misma base PostgreSQL `books-db`, publicada en **5434** por el Compose de `app-authors`. No hay todavía llamadas HTTP entre ambos servicios: cada repositorio consulta directamente la base.

## Estructura y dependencias

```text
app-books/
├── build.gradle.kts
├── README.md
├── sql/init-inventory.sql
└── src/main/
    ├── java/
    │   ├── com/programacion/distribuida/books/
    │   │   ├── db/Book.java
    │   │   ├── db/Inventory.java
    │   │   ├── repo/BookRepository.java
    │   │   └── rest/BookRest.java
    │   └── org/example/Main.java
    └── resources/
        ├── application.properties
        └── META-INF/microprofile-config.properties
```

[build.gradle.kts](build.gradle.kts) declara el plugin Quarkus y su BOM, CDI (`quarkus-arc`), Jakarta REST (`quarkus-rest`), JSON-B (`quarkus-rest-jsonb`), Hibernate ORM, Panache y el driver JDBC de PostgreSQL. Compila con UTF-8 y `-parameters`. Lombok genera getters, setters y métodos `toString()` de las entidades.

La clase `org.example.Main` es un ejemplo de consola del IDE; no inicia la API. Quarkus arranca mediante la tarea `quarkusDev` o el JAR empaquetado.

## Modelo y endpoints

| Clase | Función |
| --- | --- |
| [Book](src/main/java/com/programacion/distribuida/books/db/Book.java) | Mapea `books`: ISBN como ID `String`, título, precio `BigDecimal` y relación uno a uno con inventario. |
| [Inventory](src/main/java/com/programacion/distribuida/books/db/Inventory.java) | Mapea `inventory`: ISBN, unidades vendidas (`sold`) y suministradas (`supplied`). |
| [BookRepository](src/main/java/com/programacion/distribuida/books/repo/BookRepository.java) | Repositorio CDI transaccional que implementa `PanacheRepositoryBase<Book, String>`. |
| [BookRest](src/main/java/com/programacion/distribuida/books/rest/BookRest.java) | Recibe el repositorio por constructor y publica las consultas como JSON. |

`Inventory.bookIsbn` mapea `book_isbn`, que es clave primaria y referencia al libro. `@MapsId` hace que la asociación `Inventory.book` comparta ese identificador. En `Book`, `@OneToOne(mappedBy = "book")` declara el lado inverso de la asociación.

`@JsonbTransient` excluye la referencia `Inventory.book` de JSON para evitar la recursión libro → inventario → libro. `@ToString.Exclude` evita recorrer las referencias de la relación al imprimir las entidades.

| Endpoint | Resultado |
| --- | --- |
| `GET /books` | Lista de libros JSON; `[]` si no hay libros. |
| `GET /books/{isbn}` | Libro JSON con estado 200; estado 404 si el ISBN no existe. |

Un libro sin registro de inventario puede devolver `inventory: null`. No hay endpoints de creación, modificación o eliminación de libros o inventario. La columna SQL `version` no está mapeada con `@Version`; actualmente no hay control de concurrencia optimista mediante esa columna.

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

Dev Services está desactivado porque PostgreSQL se inicia explícitamente con Docker Compose. Hibernate no crea ni borra tablas: el esquema se prepara mediante SQL. En desarrollo puede informar diferencias del esquema mediante su comprobación en segundo plano.

Las credenciales son valores de práctica local. Para otra conexión se pueden usar `QUARKUS_DATASOURCE_JDBC_URL`, `QUARKUS_DATASOURCE_USERNAME` y `QUARKUS_DATASOURCE_PASSWORD`; esas variables tienen prioridad sobre el archivo.

[microprofile-config.properties](src/main/resources/META-INF/microprofile-config.properties) define `app.name=App Books`. Las clases actuales no consultan esa propiedad y este módulo no expone `/config`.

## Arranque paso a paso

Ejecutar los comandos desde la raíz del repositorio `Programacion-Distribuida`.

1. Seleccionar un JDK 25 completo. En el equipo de esta práctica:

   ```bash
   export JAVA_HOME="$HOME/.jdks/jbr-25.0.4.1"
   export PATH="$JAVA_HOME/bin:$PATH"
   java -version
   javac -version
   ```

   En otro equipo, sustituir la ruta por la de su JDK. Tener `java` disponible sin `javac` no basta para compilar.

2. Levantar PostgreSQL y Adminer:

   ```bash
   docker compose -f app-authors/docker-compose.yml up -d
   docker compose -f app-authors/docker-compose.yml ps
   docker compose -f app-authors/docker-compose.yml exec -T authors pg_isready -U postgres -d books-db
   ```

   Esperar a que PostgreSQL acepte conexiones. Se usa `5434` en el equipo para evitar el `5433` de la práctica `web03`. Adminer se publica en `http://localhost:9091`; su puerto interno 8080 no ocupa el puerto HTTP de libros.

3. Preparar el esquema **solo si las tablas todavía no existen**:

   ```bash
   # Solo si no existen authors, books ni books_authors.
   docker compose -f app-authors/docker-compose.yml exec -T authors psql -X -v ON_ERROR_STOP=1 -U postgres -d books-db < app-authors/sql/init.sql

   # Después, solo si no existe inventory.
   docker compose -f app-authors/docker-compose.yml exec -T authors psql -X -v ON_ERROR_STOP=1 -U postgres -d books-db < app-books/sql/init-inventory.sql
   ```

   El primer script crea y carga autores, libros y relaciones. El segundo crea inventario y carga un registro con ISBN `978-0134686097`, `sold=10` y `supplied=50`. Son inicializaciones para tablas nuevas, no migraciones ni comandos para repetir en cada arranque. No contienen borrados. Si las tablas ya existen, conservarlas y comprobar su estructura con las [referencias SQL de autores y libros](../docs/sql-autores-libros.md) y [de inventario](../docs/sql-inventario-libros.md).

4. Iniciar libros y mantener la terminal abierta:

   ```bash
   bash gradlew :app-books:quarkusDev -Ddebug=false --console=plain
   ```

   Para ejecutar autores al mismo tiempo, abrir otra terminal con el mismo JDK:

   ```bash
   bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
   ```

   `-Ddebug=false` desactiva el depurador para evitar que ambos procesos ocupen el puerto 5005 predeterminado. Los puertos HTTP siguen siendo 8080 y 8070. Véase la [guía de Quarkus con Gradle](https://quarkus.io/guides/gradle-tooling/).

5. Comprobar desde otra terminal:

   ```bash
   curl -i http://localhost:8080/books
   curl -i http://localhost:8080/books/978-0134686097
   curl -i http://localhost:8080/books/isbn-inexistente
   curl -i http://localhost:8070/authors/find/978-0134686097
   ```

   Con la carga de ejemplo, el listado contiene cinco libros. El libro `978-0134686097` incluye el inventario cargado y el ISBN inexistente devuelve 404. La consulta en 8070 permite comprobar sus autores mediante la API del otro módulo. Son comprobaciones manuales; `app-books` no contiene pruebas automatizadas propias en `src/test`.

6. Detener cada API con `Ctrl+C`. Detener los contenedores cuando ninguno de los dos servicios necesite la base:

   ```bash
   docker compose -f app-authors/docker-compose.yml stop
   ```

El Compose actual no declara un volumen con nombre. Conservar los contenedores y respaldar los datos antes de eliminarlos o recrearlos.

## Compilar y empaquetar

```bash
bash gradlew :app-authors:quarkusBuild :app-books:quarkusBuild
java -jar app-books/build/quarkus-app/quarkus-run.jar
```

PostgreSQL y las tablas deben estar preparados antes de consultar la API. Para distribuir el servicio, copiar toda la carpeta `app-books/build/quarkus-app/`, no solamente `quarkus-run.jar`.
