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

## Práctica 3: web03 — Helidon y PostgreSQL

API de libros con Helidon SE, respuesta JSON y acceso JDBC a PostgreSQL.
Incluye PostgreSQL y Adminer mediante Docker Compose.

Consultar [la documentación de web03](web03/README.md).


## Práctica 4: app-authors — Quarkus

Servicio con Quarkus, CDI, Jakarta REST, JSON-B y persistencia con Panache. Expone `GET /authors`, `GET /authors/{id}` y `GET /authors/find/{isbn}` para consultar autores. Conserva `GET /config` como ejemplo de MicroProfile Config: devuelve un cuerpo vacío y escribe los valores en consola.

```bash
bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
```

`app-authors` utiliza el puerto HTTP 8070 y PostgreSQL en `localhost:5434`. Puede ejecutarse simultáneamente con `app-books`, que utiliza el puerto HTTP 8080. Consultar [la documentación de app-authors](app-authors/README.md) para preparar la base de datos, seleccionar el JDK y ejecutar el servicio.

## Práctica 5: app-books — libros e inventario con Quarkus

API de consulta de libros e inventario con Java 25, Jakarta REST, JSON-B y Panache. Expone `GET /books` y `GET /books/{isbn}`. El inventario comparte el ISBN del libro como clave primaria y foránea mediante `@MapsId`.

```bash
bash gradlew :app-books:quarkusDev -Ddebug=false --console=plain
```

Consultar [la documentación de app-books](app-books/README.md) para configurar el JDK, preparar las tablas y ejecutar las dos APIs en terminales separadas. `-Ddebug=false` evita que ambos procesos intenten usar el puerto de depuración 5005.

| Proceso | Puerto en el equipo |
| --- | ---: |
| API de autores | 8070 |
| API de libros | 8080 |
| PostgreSQL compartido (`books-db`) | 5434 |
| Adminer de la práctica | 9091 |

La base se inicia con `docker compose -f app-authors/docker-compose.yml up -d`. Las APIs consultan directamente la misma base; todavía no se llaman entre sí por HTTP. La inicialización de tablas nuevas se realiza primero con [autores y libros](app-authors/sql/init.sql) y después con [inventario](app-books/sql/init-inventory.sql). Los scripts no deben repetirse sobre tablas ya creadas. Las referencias detalladas están en [SQL de autores y libros](docs/sql-autores-libros.md) y [SQL de inventario](docs/sql-inventario-libros.md).

## Organización del repositorio

La rama única es `main`. Integra `web01`, `web02`, `web03`, `app-authors` y `app-books`, y conserva el historial de las antiguas ramas `main` y `master` mediante un commit de fusión.

La [documentación original de master](docs/historial/README-master.md) se conserva como referencia histórica de las prácticas y su publicación. Describe el estado del equipo y del repositorio en revisiones anteriores; para la estructura y los comandos actuales, utilizar este README y los de cada módulo. Sus rutas relativas corresponden a su ubicación original en la raíz.
