-- Solo para una tabla inventory nueva; requiere los libros de app-authors/sql/init.sql.
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
