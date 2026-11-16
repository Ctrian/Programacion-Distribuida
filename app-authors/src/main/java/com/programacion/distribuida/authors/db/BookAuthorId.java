package com.programacion.distribuida.authors.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class BookAuthorId implements Serializable {

    @Column(name = "books_isbn")
    private String booksIsbn;

    @Column(name = "authors_id")
    private Long authorId;

}
