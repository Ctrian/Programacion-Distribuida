package com.programacion.distribuida.books.db;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Entity
@Table(name = "inventory")
@Getter
@Setter
@ToString
public class Inventory {

    @Id
    @Column(name = "book_isbn")
    private String bookIsbn;

    @MapsId
    @OneToOne
    @JoinColumn(name = "book_isbn")
    @JsonbTransient
    @ToString.Exclude
    private Book book;

    private Integer sold;
    private Integer supplied;
}
