package com.programacion.distribuida.web02;

import jakarta.ws.rs.SeBootstrap;

import java.net.URI;

public class Web02Main {
    public static void main(String args[]) {

        SeBootstrap.Configuration config = SeBootstrap.Configuration.builder()
                .host("0.0.0.0")
                .port(8080)
                .protocol("http")
                .build();

        SeBootstrap.start(MiAplicacionRest.class, config)
            .thenAccept(instance->{
                System.out.println(instance);
                URI uri = instance.configuration().baseUri();

                System.out.println("Servidor Iniciado en: " + uri);
            })
        .exceptionally(t -> {
                t.printStackTrace();
        return null;
        });

        /* ejecutar desde terminal desde la ruta: /home/labcom/Documentos/Distribuida/prueba/web02/build/install/web02/bin */
    }
}
