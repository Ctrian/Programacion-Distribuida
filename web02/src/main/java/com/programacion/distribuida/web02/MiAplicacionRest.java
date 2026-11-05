package com.programacion.distribuida.web02;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;


import java.util.Set;

@ApplicationPath("/api")
public class MiAplicacionRest extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                HolaRest.class
        );
    }
}
