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

`app-authors` aplica con Flyway las migraciones de la base compartida al arrancar. La guía explica cómo consultar el historial, incorporar cambios y adoptar una base inicializada manualmente. [Guía de app-authors](app-authors/README.md#migraciones-de-la-base-compartida)

## Práctica 5: app-books — libros e inventario con Quarkus

API de consulta de libros e inventario con Java 25, Jakarta REST, JSON-B y Panache. Expone `GET /books` y `GET /books/{isbn}`. El detalle obtiene los autores por HTTP consultando `GET /authors/find/{isbn}` en `app-authors`. El inventario comparte el ISBN del libro como clave primaria y foránea mediante `@MapsId`. [Guía de app-books](app-books/README.md)

### Arrancar autores y libros

Desde la raíz del repositorio, con JDK 25 y Docker Compose, iniciar PostgreSQL y comprobar que acepte conexiones.

```bash
docker compose -f app-authors/docker-compose.yml up -d
docker compose -f app-authors/docker-compose.yml exec -T authors pg_isready -U postgres -d books-db
```

Iniciar `app-authors` para que Flyway prepare `books-db`. Si la base ya incluye tablas creadas manualmente, comprobar su versión de partida antes de arrancar. [Base inicializada manualmente](app-authors/README.md#base-inicializada-manualmente)

```bash
bash gradlew :app-authors:quarkusDev -Ddebug=false --console=plain
```

Una vez que `app-authors` esté listo, iniciar `app-books` en otra terminal. `-Ddebug=false` evita que ambos procesos intenten usar el puerto de depuración 5005.

```bash
bash gradlew :app-books:quarkusDev -Ddebug=false --console=plain
```

| Proceso | Puerto en el equipo |
| --- | ---: |
| API de autores | 8070 |
| API de libros | 8080 |
| PostgreSQL compartido (`books-db`) | 5434 |
| Adminer de la práctica | 9091 |

Para una base nueva, el arranque de `app-authors` basta para cargar las tablas y los datos de ejemplo. El diseño de las tablas y las consultas de comprobación están en las referencias de [autores y libros](docs/sql-autores-libros.md) e [inventario](docs/sql-inventario-libros.md).

## Estado del proyecto

- `web01`, `web02` y `web03` se pueden ejecutar como prácticas independientes.
- `app-authors` y `app-books` comparten PostgreSQL. El esquema y la carga inicial están versionados en Flyway hasta `1.0.3`. En una base temporal con PostgreSQL 18.6 se verificó que las tres migraciones reproducen el esquema y los datos de los scripts manuales y que una segunda ejecución omite las versiones aplicadas.
- `app-books` devuelve los autores en `GET /books/{isbn}`. El listado `GET /books` consulta el servicio de autores por cada libro y aún omite los autores en la respuesta. Las dos API ofrecen consultas de lectura.

## Organización del repositorio

La rama única es `main`. Integra `web01`, `web02`, `web03`, `app-authors` y `app-books`, y conserva el historial de las antiguas ramas `main` y `master` mediante un commit de fusión.

La [documentación original de master](docs/historial/README-master.md) se conserva como referencia histórica de las prácticas y su publicación. Describe el estado del equipo y del repositorio en revisiones anteriores; para la estructura y los comandos actuales, utilizar este README y los de cada módulo. Sus rutas relativas corresponden a su ubicación original en la raíz.
