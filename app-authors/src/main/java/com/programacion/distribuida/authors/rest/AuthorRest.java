package com.programacion.distribuida.authors.rest;

import com.programacion.distribuida.authors.db.Author;
import com.programacion.distribuida.authors.repo.AuthorRepository;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/authors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthorRest {

    final AuthorRepository authorRepository;

    public AuthorRest(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GET
    public List<Author> getAllAuthors() {
        return authorRepository.listAll();
    }

    @GET
    @Path("/{id}")
    public Response getBYId(@PathParam("id") Long id) {

        return authorRepository.findByIdOptional(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/find/{isbn}")
    public List<Author> findByBook(@PathParam("isbn") String isbn) {
        return authorRepository.findByBook(isbn);
    }

}
