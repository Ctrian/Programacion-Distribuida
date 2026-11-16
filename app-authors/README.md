# app-authors — Quarkus

Práctica con Quarkus, CDI, Jakarta REST y JSON-B. El módulo utiliza Java 25 y comparte el wrapper de Gradle de la raíz del repositorio.

## Desarrollo

Desde la raíz, con un JDK 25 completo:

```bash
bash gradlew :app-authors:quarkusDev
```

El servicio escucha en el puerto 8080 por defecto. No iniciar simultáneamente otra práctica en ese puerto. Detenerlo con `Ctrl+C`.

```bash
curl -i http://localhost:8080/config
```

El recurso `TestConfigRest` devuelve actualmente una cadena vacía. No hay operaciones de autores ni conexión a una base de datos implementadas en este módulo.

## Empaquetar y ejecutar

```bash
bash gradlew :app-authors:quarkusBuild
java -jar app-authors/build/quarkus-app/quarkus-run.jar
```

Para trasladar la aplicación, copiar toda la carpeta `app-authors/build/quarkus-app/`, incluidas sus dependencias. La clase de ejemplo `org.example.Main` no es la entrada del servidor Quarkus.

La configuración de Quarkus se encuentra en `src/main/resources/application.properties`.
