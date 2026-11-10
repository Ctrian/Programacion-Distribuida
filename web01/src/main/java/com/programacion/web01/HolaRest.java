package com.programacion.web01;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.time.LocalDateTime;

@Path("/hola")
public class HolaRest {

    @GET
    @Path("/{name}")
    @Produces("text/plain")
    public String hola(@PathParam("name") String name) {
        return "hola %s - %s".formatted(name, LocalDateTime.now());
    }
}

