# Programación Distribuida

## Práctica 1: web01 — REST y despliegue WAR

Aplicación Java que expone un saludo por HTTP mediante Jakarta REST y se empaqueta con Gradle como WAR para desplegarse en un servidor externo.

- Página estática en la raíz de la aplicación.
- Recurso `GET /api/hola/{name}`: devuelve un saludo con la fecha y hora del servidor.
- Separación entre código de aplicación y entorno de ejecución; introducción al modelo cliente-servidor.

Consultar [la documentación de web01](web01/README.md) para compilar, desplegar, comprobar las rutas y entender los requisitos de compatibilidad REST del servidor.

## Compilar

Desde esta carpeta, con un JDK completo compatible con Gradle 9.8.2:

```bash
bash gradlew :web01:war
```

El resultado es `web01/build/libs/web01-1.0-SNAPSHOT.war`. El WAR no incorpora una implementación Jakarta REST; el servidor debe proporcionarla. Los fuentes y el wrapper se versionan; los archivos generados en `build/` no se incluyen.
