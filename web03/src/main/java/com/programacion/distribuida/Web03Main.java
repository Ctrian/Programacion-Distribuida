package com.programacion.distribuida;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import java.util.List;

public class Web03Main {

    public static DbClient dbClient;

    public static void handleBooks(ServerRequest req, ServerResponse res) {
        List<Book> books;
        try (var rows = dbClient.execute()
                .createQuery("select name, title from books")
                .execute()) {
            books = rows.map(row -> Book.builder()
                        .name(row.column("name").getString())
                        .title(row.column("title").getString())
                        .build()
                )
                .toList();
        }

        res.header("Content-Type", "application/json")
                .send(books);
    }

    public static void main(String[] args) {
        Config config = Config.create();


        var confighttp = config.get("server");
        Config configdb = config.get("db");

        dbClient = DbClient.builder()
                .config(configdb)
                .build();

/*
        dbClient.execute()
                .createQuery("select * from books")
                .execute()
                .map(row -> row.column("title").getString())
                .forEach(System.out::println);
*/
        // Crear servidor HTTP
        WebServer.builder()
                .config(confighttp)
                .routing(it -> it
                        .get("/books", Web03Main::handleBooks)
                )
                .build()
                .start();

    }
}
