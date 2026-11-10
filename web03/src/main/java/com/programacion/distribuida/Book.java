package com.programacion.distribuida;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Book {
    private String name;
    private String title;
}
