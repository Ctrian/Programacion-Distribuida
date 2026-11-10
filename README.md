# Programación Distribuida

## Práctica 1: web01 — REST y despliegue WAR

Aplicación Java que expone un saludo por HTTP mediante Jakarta REST y se empaqueta con Gradle como WAR para desplegarse en un servidor externo.

- Página estática en la raíz de la aplicación.
- Recurso `GET /api/hola/{name}`: devuelve un saludo con la fecha y hora del servidor.
- Separación entre código de aplicación y entorno de ejecución; introducción al modelo cliente-servidor.

Consultar [la documentación de web01](web01/README.md) para compilar, desplegar, comprobar las rutas y entender los requisitos de compatibilidad REST del servidor.

## Práctica 2: web02 — RESTEasy y Undertow embebido

La aplicación inicia su servidor HTTP desde `Web02Main`, sin desplegar un WAR en un servidor externo. RESTEasy implementa Jakarta REST y Undertow atiende las conexiones HTTP. El recurso `GET /api/hola/{name}` devuelve un saludo y la fecha y hora del servidor.

Consultar [la documentación de web02](web02/README.md) para entender la arquitectura, configurar el JDK y ejecutar la aplicación desde IntelliJ, Gradle o el lanzador generado. Esta práctica introduce el control del ciclo de vida de un servicio autónomo como base de programación distribuida.

## Compilar y ejecutar

Desde esta carpeta, con un JDK completo compatible con Gradle 9.8.2:

```bash
bash gradlew :web01:war
```

El resultado es `web01/build/libs/web01-1.0-SNAPSHOT.war`. El WAR no incorpora una implementación Jakarta REST; el servidor debe proporcionarla. Los fuentes y el wrapper se versionan; los archivos generados en `build/` no se incluyen.

Para iniciar `web02` mediante Gradle:

```bash
bash gradlew :web02:run
```

O generar su distribución y ejecutar el lanzador con sus dependencias:

```bash
bash gradlew :web02:installDist
./web02/build/install/web02/bin/web02
```

Comprobar desde otra terminal:

```bash
curl -i http://localhost:8080/api/hola/Ana
```

Se espera estado `200` y el texto `hola Ana - ...`. `web01` y `web02` utilizan el puerto 8080 en las configuraciones de ejemplo; ejecutar una práctica a la vez o configurar puertos distintos. Detener `web02` con `Ctrl+C` si se arrancó desde terminal.
