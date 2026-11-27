# SQL de referencia: inventario de app-books

Flyway crea y carga `inventory` al arrancar `app-authors`. La versión `1.0.3` depende de `1.0.2`, que crea `books` y carga el ISBN de ejemplo. [Migración de inventario](../app-authors/src/main/resources/db/migration/V1.0.3__crear-tablas-inventory.sql) · [Guía de migraciones](../app-authors/README.md#migraciones-de-la-base-compartida)

El documento conserva el SQL original, explica las restricciones y muestra cómo comprobar la carga. Si una base sin historial ya contiene el esquema y los datos de referencia de autores, libros, relaciones e inventario, verificar su equivalencia con `1.0.3` antes de registrarla con ese baseline. [Script manual de referencia](../app-books/sql/init-inventory.sql) · [SQL de autores y libros](sql-autores-libros.md) · [Flujo de baseline](../app-authors/README.md#base-inicializada-manualmente)

## SQL original recibido

Se normalizan los espacios y saltos de línea del mensaje para mostrar SQL que se pueda copiar:

```sql
CREATE TABLE public.inventory (
    sold integer NOT NULL,
    supplied integer NOT NULL,
    version integer NOT NULL,
    book_isbn varchar(255) NOT NULL
);

INSERT INTO public.inventory (sold, supplied, version, book_isbn) OVERRIDING SYSTEM VALUE
VALUES (10, 50, 1, '978-0134686097');
```

La sintaxis es válida en PostgreSQL si la tabla todavía no existe. `OVERRIDING SYSTEM VALUE` es innecesario aquí porque ninguna columna es identity. No es un error de sintaxis; se elimina en la propuesta para simplificarla. Su función es permitir valores explícitos para columnas `GENERATED ALWAYS AS IDENTITY`. Véase la [referencia de INSERT](https://www.postgresql.org/docs/current/sql-insert.html).

## Columnas y significado

| Columna | Tipo | Significado usado en este ejemplo |
| --- | --- | --- |
| `sold` | `integer` | Unidades vendidas acumuladas: `10`. |
| `supplied` | `integer` | Unidades suministradas acumuladas: `50`. |
| `version` | `integer` | Valor inicial `1`. El nombre de la columna no activa por sí solo el versionado de JPA. |
| `book_isbn` | `varchar(255)` | ISBN del libro al que corresponde el inventario. |

Si `supplied` y `sold` representan acumulados del mismo inventario, las existencias disponibles son `supplied - sold`: **40 unidades** para el registro proporcionado. Bajo esa interpretación tiene sentido impedir valores negativos y ventas superiores a las unidades suministradas; esas reglas se añaden explícitamente en la propuesta.

## Correcciones y mejoras

| Hallazgo | Propuesta |
| --- | --- |
| No hay clave primaria. | Usar `book_isbn` como PK para permitir como máximo un registro de inventario por libro, coherente con la relación uno a uno del modelo Java. |
| No hay clave foránea. | Referenciar `public.books(isbn)` para impedir inventarios de libros inexistentes. |
| No se restringen cantidades negativas. | Añadir `CHECK (sold >= 0)` y `CHECK (supplied >= 0)`. |
| No se comprueba la disponibilidad. | Añadir `CHECK (sold <= supplied)` si se adopta la interpretación de acumulados indicada arriba. |
| `version` es obligatorio y no tiene valor predeterminado. | Añadir `DEFAULT 1` porque la entidad `Inventory` actual no mapea esta columna. |

Las PK, FK y condiciones `CHECK` son mejoras de integridad respecto al SQL original; no son necesarias para que la sintaxis original sea válida. Véase la [documentación de restricciones de PostgreSQL](https://www.postgresql.org/docs/current/ddl-constraints.html).

## SQL de inicialización manual de referencia

El bloque histórico se ejecutaba tras la carga manual de libros y exigía estas condiciones:

- `public.books` existe en la misma base y `isbn` es una clave primaria o única de tipo `varchar(255)`.
- El libro con ISBN `978-0134686097` existe antes del `INSERT`.
- `public.inventory` se crea por primera vez.

En el flujo actual, Flyway ejecuta `V1.0.3` después de `V1.0.2` y abre y cierra la transacción de la migración.

```sql
BEGIN;

CREATE TABLE public.inventory (
    sold integer NOT NULL DEFAULT 0,
    supplied integer NOT NULL DEFAULT 0,
    version integer NOT NULL DEFAULT 1,
    book_isbn varchar(255) NOT NULL,
    CONSTRAINT inventory_pkey PRIMARY KEY (book_isbn),
    CONSTRAINT inventory_book_fk
        FOREIGN KEY (book_isbn) REFERENCES public.books (isbn),
    CONSTRAINT inventory_sold_nonnegative CHECK (sold >= 0),
    CONSTRAINT inventory_supplied_nonnegative CHECK (supplied >= 0),
    CONSTRAINT inventory_stock_valid CHECK (sold <= supplied)
);

INSERT INTO public.inventory (sold, supplied, version, book_isbn)
VALUES (10, 50, 1, '978-0134686097');

COMMIT;
```

Se conservan exactamente los cuatro valores recibidos. Los valores predeterminados solo se aplican cuando una columna se omite o se usa `DEFAULT`; no reemplazan un `NULL` enviado explícitamente. Con la FK predeterminada, no se puede borrar un libro mientras tenga este inventario asociado.

## Correspondencia con las entidades Java

- `Inventory` declara `@Table(name = "inventory")`, el ID `String bookIsbn` con `@Column(name = "book_isbn")` y la asociación `book` con `@MapsId`, `@OneToOne` y `@JoinColumn(name = "book_isbn")`. El SQL representa esa identidad mediante `book_isbn`, que es simultáneamente PK y FK. [Inventory.java](../app-books/src/main/java/com/programacion/distribuida/books/db/Inventory.java)
- Los campos `sold` y `supplied` son `Integer`, coherentes con `integer` de PostgreSQL.
- `Inventory` no declara un campo `version` ni `@Version`. El `DEFAULT 1` permite insertar desde Java sin proporcionar esa columna, pero no implementa control de concurrencia optimista. Si se quiere ese control, habrá que mapear la columna con `@Version`.
- `Book` declara `isbn` como `String` con `@Id` e `inventory` con `@OneToOne(mappedBy = "book")` para el lado inverso de la relación. La API expone las cantidades como `inventorySold` e `inventorySupplied` mediante `BookDto`. [Book.java](../app-books/src/main/java/com/programacion/distribuida/books/db/Book.java) · [BookDto.java](../app-books/src/main/java/com/programacion/distribuida/books/dtos/BookDto.java)

La API ofrece consultas de lectura. Los resultados SQL que siguen corresponden a los datos cargados por `V1.0.3` en una base nueva.

## Consultas para comprobar la carga

```sql
-- Comprobar primero que el libro existe.
SELECT isbn, title
FROM public.books
WHERE isbn = '978-0134686097';

-- Comprobar el inventario y calcular las unidades disponibles.
SELECT book_isbn, sold, supplied, version,
       supplied - sold AS available
FROM public.inventory
WHERE book_isbn = '978-0134686097';
-- Esperado: sold=10, supplied=50, version=1, available=40.

-- Mostrar el libro junto con su inventario.
SELECT b.isbn, b.title, i.sold, i.supplied,
       i.supplied - i.sold AS available
FROM public.books AS b
JOIN public.inventory AS i ON i.book_isbn = b.isbn
WHERE b.isbn = '978-0134686097';
```

Las consultas de comprobación se pueden ejecutar en Adminer o `psql`, conectados a la base que migró `app-authors`.
