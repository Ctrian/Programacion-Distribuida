package com.programacion.distribuida.books.clients;

import com.programacion.distribuida.books.dtos.AuthorDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/authors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RegisterRestClient(baseUri = "http://localhost:8070")
@RegisterRestClient(configKey = "AuthorRestClient")
public interface AuthorRestClient {
//    @GET
//    public List<AuthorDto> getAll();
//
//    @GET
//    @Path("/{id}")
//    public Response getBYId(@PathParam("id") Long id);

    @GET
    @Path("/find/{isbn}")
    public List<AuthorDto> findByBook(@PathParam("isbn") String isbn);

}