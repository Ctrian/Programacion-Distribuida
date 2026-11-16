package com.programacion.distribuida.authors.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/config")
public class TestConfigRest {

    @GET
    public String test(){
        return "";
    }
}
