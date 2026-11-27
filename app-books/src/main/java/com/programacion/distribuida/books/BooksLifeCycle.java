package com.programacion.distribuida.books;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class BooksLifeCycle {

    @Inject
    @ConfigProperty(name = "consul.host", defaultValue = "127.0.0.1")
    String consulHost;

    @Inject
    @ConfigProperty(name = "consul.port", defaultValue = "8500")
    Integer consulPort;

    @Inject
    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8070")
    int appPort;

    @Inject
    @ConfigProperty(name = "app.name")
    String appName;

    private String serviceId;

    void init(@Observes StartupEvent event, Vertx vertx) {
        if (appPort == 0) {
            System.err.println("Configura un puerto HTTP fijo para registrar app-authors en Consul");
            return;
        }

        String address = "127.0.0.1";
        String id = "%s-%s:%d".formatted(appName, address, appPort);
        ConsulClient client = null;
        try {
            client = createClient(vertx);
            ServiceOptions serviceOptions = new ServiceOptions()
                    .setName(appName)
                    .setId(id)
                    .setAddress(address)
                    .setPort(appPort);

            client.registerService(serviceOptions)
                    .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            serviceId = id;
            System.out.println("Servicio registrado en Consul: " + serviceId);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException | RuntimeException error) {
            System.err.printf("No se pudo registrar %s en Consul (%s:%d): %s%n",
                    id, consulHost, consulPort, error);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    void destroy(@Observes ShutdownEvent event, Vertx vertx) {
        if (serviceId == null) {
            return;
        }

        ConsulClient client = null;
        try {
            client = createClient(vertx);
            client.deregisterService(serviceId)
                    .toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
            System.out.println("Servicio retirado de Consul: " + serviceId);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException | RuntimeException error) {
            System.err.printf("No se pudo retirar %s de Consul: %s%n", serviceId, error);
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private ConsulClient createClient(Vertx vertx) {
        ConsulClientOptions options = new ConsulClientOptions()
                .setHost(consulHost)
                .setPort(consulPort);
        return ConsulClient.create(vertx, options);
    }
}
