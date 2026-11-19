package com.programacion.distribuida.books.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
public class BookDto {
    private String isbn;
    private String title;
    private BigDecimal price;
    private Integer inventorySold;
    private Integer inventorySupplied;

    private List<AuthorDto> authors;
}