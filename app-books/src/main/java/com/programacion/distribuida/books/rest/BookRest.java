package com.programacion.distribuida.books.rest;

import com.programacion.distribuida.books.db.Book;
import com.programacion.distribuida.books.repo.BookRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookRest {

    final BookRepository bookRepository;

    @Inject
    public BookRest(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GET
    public List<Book> findAll() {
            return bookRepository.listAll();
        }

    @GET
    @Path("/{isbn}")
    public Response getBYId(@PathParam("isbn") String isbn) {

        return bookRepository.findByIdOptional(isbn)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }
}
