CREATE TABLE public.books (
    price numeric(8,2) NOT NULL,
    version integer NOT NULL DEFAULT 1,
    isbn varchar(255) PRIMARY KEY,
    title varchar(255)
);

INSERT INTO public.books (price, version, isbn, title) VALUES
    (40.00, 1, '978-0596009205', 'Head First Java'),
    (55.00, 1, '978-0321349606', 'Java Concurrency in Practice'),
    (50.00, 1, '978-0136083238', 'Clean Code: A Handbook of Agile Software Craftsmanship'),
    (45.00, 1, '978-0134686097', 'Effective Java'),
    (50.00, 1, '978-1260440232', 'Java: The Complete Reference');

-- Un autor puede tener varios libros y un libro puede tener varios autores.
CREATE TABLE public.books_authors (
    authors_id bigint NOT NULL,
    books_isbn varchar(255) NOT NULL,
    CONSTRAINT books_authors_pkey PRIMARY KEY (authors_id, books_isbn),
    CONSTRAINT books_authors_author_fk
        FOREIGN KEY (authors_id) REFERENCES public.authors (id),
    CONSTRAINT books_authors_book_fk
        FOREIGN KEY (books_isbn) REFERENCES public.books (isbn)
);

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
