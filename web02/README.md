# web02 — RESTEasy con Undertow embebido

Segunda práctica: la propia aplicación arranca el servidor HTTP desde un método `main`, en lugar de desplegar un WAR en un servidor externo.

Los comandos se ejecutan desde la raíz Gradle, donde están `gradlew` y `settings.gradle.kts`: `laboratorio/` en el entorno original o la carpeta del repositorio clonado. Las referencias a `laboratorio/` en los pasos siguientes indican esa raíz.

## Responsabilidad de cada pieza

| Pieza | Responsabilidad |
| --- | --- |
| Jakarta REST | Define la API estándar: `@Path`, `@GET`, `Application`, `SeBootstrap`, etc. |
| RESTEasy | Implementa Jakarta REST: relaciona las peticiones con los recursos y ejecuta los métodos REST. |
| Undertow | Atiende las conexiones HTTP; puede integrarse como servidor embebido. |
| `resteasy-undertow` | Integra RESTEasy con Undertow. |
| `Web02Main` | Configura e inicia la aplicación mediante `SeBootstrap`. |

`resteasy-core` contiene funcionalidad central de RESTEasy; **el nombre del artefacto no significa Jakarta EE Core Profile**. Undertow tampoco es Web Profile. Los perfiles son conjuntos de especificaciones; combinar estas dos bibliotecas no convierte automáticamente la aplicación en un servidor Jakarta EE completo.

## Flujo de una petición

```text
Cliente -- HTTP --> Undertow --> RESTEasy --> HolaRest.hola(name)
Cliente <-- texto de respuesta <-------------+
```

`MiAplicacionRest` establece `/api` y registra `HolaRest`. `Web02Main` escucha en `0.0.0.0:8080`. Para acceder desde el mismo equipo se utiliza `localhost`; desde otro, la IP del servidor y un puerto accesible.

## Ejecución en IntelliJ IDEA

1. Abrir `laboratorio/` y sincronizar el proyecto Gradle.
2. Configurar un JDK compatible con las dependencias declaradas.
3. Abrir `Web02Main.java` y ejecutar `com.programacion.distribuida.web02.Web02Main` con el botón junto a `main`.
4. Probar desde una terminal:

```bash
curl http://localhost:8080/api/hola/Ana
```

La respuesta incluye `hola Ana` y la fecha y hora del servidor. Detener las otras prácticas si ya ocupan el puerto 8080.

### Comprobar la respuesta HTTP paso a paso

1. Detener Tomcat, `web03` o cualquier otra aplicación que ocupe el puerto 8080.
2. Ejecutar `Web02Main` desde IntelliJ y mantenerlo en ejecución. Esperar al mensaje `Servidor Iniciado en:`; si aparece una excepción, resolverla antes de probar la petición.
3. Abrir `http://localhost:8080/api/hola/Ana` en el navegador. Debe mostrarse un saludo con el nombre y una fecha y hora.
4. Para comprobar también el estado y las cabeceras, ejecutar en otra terminal:

   ```bash
   curl -i --max-time 10 http://localhost:8080/api/hola/Ana
   ```

   El resultado debe tener estado `200`, tipo de contenido `text/plain` (puede incluir un charset) y un cuerpo parecido a `hola Ana - 2026-11-09T21:30:00`. La fecha y hora reales serán las del servidor al atender la petición.

5. Repetir con `/api/hola/Luis`: el cuerpo debe cambiar a `hola Luis - ...`. Esto comprueba que el recurso recibe el parámetro de la URL.
6. Detener la aplicación con el botón Stop de IntelliJ. Si se ejecutó con Gradle desde la terminal, utilizar `Ctrl+C`.

Si `curl` indica que no puede conectar, comprobar que el proceso sigue activo y que no falló el arranque. Un `404` significa que hubo respuesta HTTP, pero no se encontró esa ruta: comprobar `/api/hola/Ana` y que quien escucha sea `web02`. Un error de puerto ocupado requiere detener el proceso que ya utiliza el 8080.

## Ejecución con Gradle

El plugin `application` necesita saber qué clase contiene el método `main` que debe ejecutar. El `build.gradle.kts` ya declara esa entrada:

```kotlin
application {
    mainClass.set("com.programacion.distribuida.web02.Web02Main")
}
```

Desde `laboratorio/`, iniciar la aplicación con:

```bash
bash gradlew :web02:run
```

### Ejecutar el lanzador generado en build/

Esta es una alternativa a `:web02:run`: Gradle prepara la distribución y después se ejecuta un script que arranca Java directamente. Primero, desde `laboratorio/`, generar los archivos:

```bash
bash gradlew :web02:installDist
```

La tarea crea esta estructura:

```text
web02/build/install/web02/
├── bin/
│   ├── web02       # lanzador para Linux/macOS
│   └── web02.bat   # lanzador para Windows
└── lib/
    ├── web02-1.0-SNAPSHOT.jar
    └── ...         # dependencias, incluidas RESTEasy y Undertow
```

Entrar a `bin/` y ejecutar el lanzador:

```bash
cd web02/build/install/web02/bin
./web02
```

También puede ejecutarse desde `laboratorio/` sin cambiar de directorio:

```bash
./web02/build/install/web02/bin/web02
```

`./` indica que el ejecutable está en el directorio actual. El comando es `./web02`, no `.web02`. **No se añade la clase principal después del comando**: el script generado ya conoce `com.programacion.distribuida.web02.Web02Main` gracias a `application.mainClass`, y configura el classpath con los JAR de `lib/`. Los argumentos que se escribieran después de `./web02` se pasarían al `main` de la aplicación.

Para mover la distribución a otro directorio o equipo, copiar toda la carpeta `build/install/web02/`, conservando `bin/` y `lib/`. El destino necesita una instalación Java compatible; el lanzador no incluye una JVM. No hace falta instalar Gradle para ejecutar la distribución ya generada.

Cuando cambie el código, volver a ejecutar `:web02:installDist`: el lanzador utiliza los archivos compilados de la distribución, no recompila los fuentes automáticamente. Un JAR ordinario de `build/libs/` no incluye automáticamente todas las dependencias ni equivale a esta distribución.

Dejar el proceso abierto y comprobarlo desde otra terminal con `curl -i http://localhost:8080/api/hola/Ana`. Se detiene con `Ctrl+C`, igual que `:web02:run`. Ambas formas inician el mismo servidor; elegir una y evitar arrancarlas simultáneamente sobre el puerto 8080.

| Forma | Qué utiliza para arrancar | Uso |
| --- | --- | --- |
| Run sobre `Web02Main` en IntelliJ | Configuración Java del IDE | Desarrollo y depuración |
| `bash gradlew :web02:run` | Tarea `run` de Gradle | Desarrollo desde terminal |
| `./web02` desde `build/install/web02/bin/` | Lanzador y dependencias de la distribución | Ejecutar la aplicación ya empaquetada |

En IntelliJ también pueden ejecutarse las tareas `run` e `installDist` del módulo desde la ventana Gradle.

### JDK utilizado por Gradle

La terminal y el IDE pueden utilizar instalaciones de Java diferentes. Si Gradle informa que Java no proporciona `JAVA_COMPILER`, comprobar que se está utilizando un JDK completo, con `bin/javac`, y no solamente un entorno de ejecución.

En el equipo revisado, la compilación se verificó con el JDK de IntelliJ ubicado en `/home/oli/.jdks/jbr-25.0.4.1`:

```bash
JAVA_HOME=/home/oli/.jdks/jbr-25.0.4.1 bash gradlew :web02:installDist --offline --console=plain
```

Esta ruta es local: en otro equipo se debe sustituir por la de su JDK. `--offline` utiliza las dependencias de la caché; omitirlo si todavía deben descargarse. En IntelliJ, seleccionar un JDK completo como **Gradle JVM** en la configuración de Gradle.

Para seleccionar ese JDK durante toda la sesión de terminal, en vez de repetirlo en cada comando:

```bash
export JAVA_HOME=/home/oli/.jdks/jbr-25.0.4.1
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
```

Ejecutar los comandos Gradle o el lanzador en esa misma terminal. Una asignación como `JAVA_HOME=... bash gradlew ...` solo se aplica a ese comando y no cambia el Java de un lanzador ejecutado después.

**Verificación realizada:** `:web02:installDist` terminó correctamente y generó el JAR, los scripts y la distribución. No se arrancó el servidor ni se comprobó el endpoint HTTP en esta verificación.

**Prueba manual confirmada por el usuario:** se vio el saludo en `/api/hola/Ana`, por lo que la comprobación manual del recurso REST de `web02` queda confirmada. El literal `ok` correspondía a la versión demostrativa de `/books` de `web03`. Para repetir la comprobación básica, confirmar estado `200`, tipo `text/plain` y que el nombre cambia al cambiar la URL. No es necesario añadir más funcionalidad para dar por válida esa demostración.

### Corrección aplicada

Se añadió `application { mainClass.set("com.programacion.distribuida.web02.Web02Main") }` a `build.gradle.kts`. Antes, el plugin `application` no tenía indicada la clase de entrada para sus tareas y lanzadores. No se cambió el método `main` ni la lógica del recurso REST como parte de esta corrección. El nombre de paquete incluye `.web02`; `com.programacion.distribuida.Web02Main` no es la clase de este módulo.

## Relación con programación distribuida

Se mantiene el contrato HTTP de la primera práctica, pero cambia quién controla el servidor: ahora lo inicia la aplicación. Esto facilita ejecutar instancias como procesos independientes y prepara el camino para servicios autónomos. Tener un servidor embebido no basta para afirmar que existe una arquitectura de microservicios: también importan los límites y la interacción entre servicios.

## Referencias

- [RESTEasy: SeBootstrap y contenedores embebidos](https://docs.resteasy.dev/7.0/userguide/).
- [Undertow: servidor HTTP embebible](https://undertow.io/).
- [Jakarta EE Core Profile](https://jakarta.ee/specifications/coreprofile/11/) y [Web Profile](https://jakarta.ee/specifications/webprofile/11/).
