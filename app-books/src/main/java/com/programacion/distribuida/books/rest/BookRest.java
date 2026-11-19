package com.programacion.distribuida.books.rest;

import com.programacion.distribuida.books.dtos.BookDto;
import com.programacion.distribuida.books.service.BookService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookRest {

    final BookService bookService;

    @Inject
    public BookRest(BookService bookService) {
        this.bookService = bookService;
    }

    @GET
    public List<BookDto> findAll() {
            return bookService.findAll();
        }

    @GET
    @Path("/{isbn}")
    public Response getBYId(@PathParam("isbn") String isbn) {
        return bookService.findByIdOptional(isbn)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }
}
