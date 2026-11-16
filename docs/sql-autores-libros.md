# SQL de referencia: autores, libros y sus relaciones

Este documento conserva el SQL proporcionado para PostgreSQL, explica los problemas encontrados y propone una versión completa para una base de práctica nueva. Se puede consultar desde cualquier editor, sin las herramientas de base de datos de IntelliJ IDEA Ultimate.

El SQL original se conserva como referencia de la revisión. La propuesta completa también está disponible en [app-authors/sql/init.sql](../app-authors/sql/init.sql) y se utilizó para preparar una base local de práctica. Leer este archivo no modifica tablas ni datos; la inicialización requiere ejecutar el script explícitamente y solo sobre tablas nuevas. La revisión contrasta las sentencias con la documentación de PostgreSQL y con las entidades Java presentes en `app-authors`.

## SQL original recibido

Se conserva tal como fue proporcionado, incluido el orden de las operaciones y la ausencia de punto y coma en los dos `CREATE TABLE`. **Este bloque sirve de registro; para preparar una base nueva, usar la propuesta posterior.**

```sql
CREATE TABLE public.authors (
    version integer NOT NULL,
    id bigint NOT NULL,
    name varchar(255)
)

CREATE TABLE public.books (
    price numeric(8,2) NOT NULL,
    version integer NOT NULL,
    isbn varchar(255) NOT NULL,
    title varchar(255)
)

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (1, '978-0134686097');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (2, '978-1260440232');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (3, '978-0596009205');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (4, '978-0321349606');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (5, '978-0136083238');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (5, '978-0134686097');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (1, '978-1260440232');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (2, '978-0596009205');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (3, '978-0321349606');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (4, '978-0136083238');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (1, '978-0136083238');

INSERT INTO public.books_authors (authors_id, books_isbn) OVERRIDING SYSTEM VALUE
 VALUES (2, '978-0136083238');

INSERT INTO public.books (price, version, isbn, title) OVERRIDING SYSTEM VALUE
 VALUES (40.00, 1, '978-0596009205', 'Head First Java');

INSERT INTO public.books (price, version, isbn, title) OVERRIDING SYSTEM VALUE
 VALUES (55.00, 1, '978-0321349606', 'Java Concurrency in Practice');

INSERT INTO public.books (price, version, isbn, title) OVERRIDING SYSTEM VALUE
 VALUES (50.00, 1, '978-0136083238', 'Clean Code: A Handbook of Agile Software Craftsmanship');

INSERT INTO public.books (price, version, isbn, title) OVERRIDING SYSTEM VALUE
 VALUES (45.00, 1, '978-0134686097', 'Effective Java');

INSERT INTO public.books (price, version, isbn, title) OVERRIDING SYSTEM VALUE
 VALUES (50.00, 1, '978-1260440232', 'Java: The Complete Reference');
```

## Errores, omisiones y mejoras

| Hallazgo | Efecto y corrección |
| --- | --- |
| Faltan `;` al terminar los dos `CREATE TABLE`. | Al ejecutar el texto como un script se mezclan instrucciones y falla la sintaxis. Terminar ambos con `);`. |
| No se define `public.books_authors`. | Los `INSERT` fallan si la tabla no existe previamente. Añadir su definición. |
| No hay inserciones de autores. | Con claves foráneas, los IDs del 1 al 5 deben existir antes de insertar relaciones. Añadir los autores reales o datos de práctica explícitos. |
| Las relaciones se insertan antes que los libros. | Con claves foráneas de comprobación inmediata, los ISBN deben existir primero. Insertar autores y libros antes de sus relaciones. |
| No se declaran claves primarias. | Es SQL válido, pero permite IDs o ISBN repetidos. Definir una PK para `authors.id`, otra para `books.isbn` y una compuesta para cada pareja de la tabla intermedia. |
| No se declaran claves foráneas. | Sin ellas pueden existir relaciones hacia autores o libros inexistentes. Añadir referencias a las claves primarias. |
| `authors.id` no tiene generación automática. | El SQL original exige proporcionar el ID. Si se desea usar `GenerationType.IDENTITY`, definir una columna identity. |
| `OVERRIDING SYSTEM VALUE` aparece en todos los `INSERT`. | Es sintaxis válida de PostgreSQL, pero no aporta nada para las columnas de `books` mostradas ni para las dos referencias de la tabla intermedia: no son identity. Se puede omitir. |

`OVERRIDING SYSTEM VALUE` permite introducir valores explícitos en una columna `GENERATED ALWAYS AS IDENTITY`; con `GENERATED BY DEFAULT AS IDENTITY`, ya se permiten valores explícitos sin esta cláusula. Véase la [referencia de INSERT](https://www.postgresql.org/docs/current/sql-insert.html).

Los cinco precios caben en `numeric(8,2)`. Los cinco ISBN usados por las doce relaciones aparecen en las inserciones de libros y no hay parejas autor–ISBN repetidas. Esto verifica la coherencia interna de los datos; no comprueba que cada ISBN corresponda al título indicado ni que las relaciones representen las autorías reales.

## Propuesta completa para una base nueva

Esta propuesta mantiene `bigint`, los nombres de columnas y los datos de libros y relaciones del SQL recibido. Añade PK, FK, generación de IDs y un valor predeterminado de `1` para `version`.

**Los nombres `Autor de ejemplo 1` a `Autor de ejemplo 5` son marcadores de práctica añadidos para completar la carga. Sustituirlos por los nombres correctos antes de usar datos reales.**

Ejecutar una sola vez en una base de práctica donde estas tres tablas todavía no existan. No es una migración de tablas existentes ni un script para ejecutar repetidamente: fallará si las tablas ya están creadas. No contiene `DROP`, `TRUNCATE` ni borrados.

```sql
BEGIN;

-- 1. Tablas principales.
CREATE TABLE public.authors (
    version integer NOT NULL DEFAULT 1,
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name varchar(255)
);

CREATE TABLE public.books (
    price numeric(8,2) NOT NULL,
    version integer NOT NULL DEFAULT 1,
    isbn varchar(255) PRIMARY KEY,
    title varchar(255)
);

-- 2. Tabla intermedia: un autor puede tener varios libros y viceversa.
CREATE TABLE public.books_authors (
    authors_id bigint NOT NULL,
    books_isbn varchar(255) NOT NULL,
    CONSTRAINT books_authors_pkey PRIMARY KEY (authors_id, books_isbn),
    CONSTRAINT books_authors_author_fk
        FOREIGN KEY (authors_id) REFERENCES public.authors (id),
    CONSTRAINT books_authors_book_fk
        FOREIGN KEY (books_isbn) REFERENCES public.books (isbn)
);

-- 3. Autores de práctica. Los nombres reales no fueron proporcionados.
INSERT INTO public.authors (version, id, name) VALUES
    (1, 1, 'Autor de ejemplo 1'),
    (1, 2, 'Autor de ejemplo 2'),
    (1, 3, 'Autor de ejemplo 3'),
    (1, 4, 'Autor de ejemplo 4'),
    (1, 5, 'Autor de ejemplo 5');

-- Los IDs explícitos no avanzan la secuencia identity.
-- Sincronizarla con el máximo para que el próximo ID automático sea 6.
SELECT setval(
    pg_get_serial_sequence('public.authors', 'id'),
    (SELECT MAX(id) FROM public.authors),
    true
);

-- 4. Libros: se conservan los valores recibidos.
INSERT INTO public.books (price, version, isbn, title) VALUES
    (40.00, 1, '978-0596009205', 'Head First Java'),
    (55.00, 1, '978-0321349606', 'Java Concurrency in Practice'),
    (50.00, 1, '978-0136083238', 'Clean Code: A Handbook of Agile Software Craftsmanship'),
    (45.00, 1, '978-0134686097', 'Effective Java'),
    (50.00, 1, '978-1260440232', 'Java: The Complete Reference');

-- 5. Relaciones: los autores y libros referenciados ya existen.
INSERT INTO public.books_authors (authors_id, books_isbn) VALUES
    (1, '978-0134686097'),
    (2, '978-1260440232'),
    (3, '978-0596009205'),
    (4, '978-0321349606'),
    (5, '978-0136083238'),
    (5, '978-0134686097'),
    (1, '978-1260440232'),
    (2, '978-0596009205'),
    (3, '978-0321349606'),
    (4, '978-0136083238'),
    (1, '978-0136083238'),
    (2, '978-0136083238');

COMMIT;
```

La PK compuesta evita repetir una pareja autor–ISBN. Las FK impiden insertar referencias inexistentes y, con el comportamiento predeterminado, impiden borrar un autor o libro que todavía tenga relaciones. `version` es una columna ordinaria: nombrarla así no activa por sí solo el control de concurrencia de JPA; eso requiere un mapeo con `@Version`.

## Diferencias detectadas en la revisión inicial

La siguiente tabla conserva los hallazgos de la revisión inicial. Las clases actuales de `app-authors` ya alinean `books_authors`, `authors_id`, IDs `Long`, igualdad de la clave compuesta y la consulta `o.id.booksIsbn`. La columna `version` continúa sin mapearse en Java; el SQL usa `DEFAULT 1`. La propuesta completa también está disponible como `app-authors/sql/init.sql` para inicializar una base nueva.

| Archivo | Diferencia observada |
| --- | --- |
| [BookAuthor.java](../app-authors/src/main/java/com/programacion/distribuida/authors/db/BookAuthor.java) | Usa `@Table(name = "book_authors")`, mientras el SQL usa `books_authors`. Elegir un mismo nombre en ambos lugares. |
| [BookAuthorId.java](../app-authors/src/main/java/com/programacion/distribuida/authors/db/BookAuthorId.java) | Usa `@Column(name = "author_id")`, mientras el SQL y el `@JoinColumn` de `BookAuthor` usan `authors_id`. Unificar el nombre. |
| [Author.java](../app-authors/src/main/java/com/programacion/distribuida/authors/db/Author.java) | Declara `Integer id`, pero el SQL recibido y la propuesta usan `bigint`. Para conservar `bigint`, alinear el modelo a `Long`, incluidos el ID compuesto y el tipo de ID del repositorio; alternativamente, elegir `integer` de forma consistente en el SQL. |
| [Author.java](../app-authors/src/main/java/com/programacion/distribuida/authors/db/Author.java) | Usa `GenerationType.IDENTITY`. La propuesta añade identity; el SQL original no la define. Tampoco hay un campo `version` mapeado en esta entidad: el `DEFAULT 1` de la propuesta evita exigirlo al insertar desde la aplicación, pero no implementa versionado JPA. |
| [BookAuthorId.java](../app-authors/src/main/java/com/programacion/distribuida/authors/db/BookAuthorId.java) | La clave compuesta no declara `equals()` ni `hashCode()`; conviene implementarlos al completar el mapeo JPA. |
| [AuthorRepository.java](../app-authors/src/main/java/com/programacion/distribuida/authors/repo/AuthorRepository.java) | La consulta usa `o.id.book.isbn`, pero el ID declara `booksIsbn`, sin propiedad `book`. La ruta coherente con los campos actuales es `o.id.booksIsbn`. |

## Consultas para comprobar los datos

Después de ejecutar correctamente la propuesta sobre tablas nuevas, se esperan 5 autores, 5 libros y 12 relaciones:

```sql
SELECT 'authors' AS tabla, COUNT(*) AS filas FROM public.authors
UNION ALL
SELECT 'books', COUNT(*) FROM public.books
UNION ALL
SELECT 'books_authors', COUNT(*) FROM public.books_authors;

SELECT a.id, a.name, b.isbn, b.title, b.price
FROM public.books_authors AS ba
JOIN public.authors AS a ON a.id = ba.authors_id
JOIN public.books AS b ON b.isbn = ba.books_isbn
ORDER BY b.isbn, a.id;

-- Autores relacionados con un libro concreto.
SELECT a.id, a.name
FROM public.authors AS a
JOIN public.books_authors AS ba ON ba.authors_id = a.id
WHERE ba.books_isbn = '978-0136083238'
ORDER BY a.id;
-- Resultado esperado para esta carga: IDs 1, 2, 4 y 5.
```

Se pueden copiar los bloques a un cliente PostgreSQL como `psql`. Si se guarda únicamente el bloque de creación y carga en un archivo `.sql`, se puede ejecutar con `psql -X -v ON_ERROR_STOP=1 -h HOST -p PUERTO -U USUARIO -d BASE -f archivo.sql`, sustituyendo los parámetros por los de la conexión. El archivo Markdown completo no debe pasarse a `psql`.

## Referencias de PostgreSQL

- [CREATE TABLE: columnas identity y restricciones](https://www.postgresql.org/docs/current/sql-createtable.html).
- [Restricciones: claves primarias y foráneas](https://www.postgresql.org/docs/current/ddl-constraints.html).
- [INSERT: OVERRIDING SYSTEM VALUE](https://www.postgresql.org/docs/current/sql-insert.html).
- [Funciones de secuencia: setval](https://www.postgresql.org/docs/current/functions-sequence.html).
