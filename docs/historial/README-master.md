# Programación Distribuida — Laboratorios Java

Tres prácticas que muestran la evolución del despliegue y la comunicación entre procesos:

| Módulo | Modelo | Comunicación |
| --- | --- | --- |
| [web01](web01/README.md) | WAR en un servidor externo | Cliente → HTTP → recurso REST |
| [web02](web02/README.md) | RESTEasy y Undertow embebido | Cliente → HTTP → aplicación autónoma |
| [web03](web03/README.md) | Helidon SE y PostgreSQL en Docker | Cliente → HTTP → aplicación → JDBC → base de datos |

Los módulos comparten el wrapper y la configuración de un proyecto Gradle. Debe abrirse esta carpeta como proyecto en IntelliJ IDEA. Cada README describe el estado real de su práctica y los ajustes pendientes; no se presupone que las tres se ejecuten simultáneamente, porque utilizan el puerto 8080.

## Cómo se relacionan con programación distribuida

La progresión es: exponer una funcionalidad por red, controlar el ciclo de vida del servicio y conectar procesos independientes para acceder a datos. Un cliente y un servidor pueden estar en máquinas diferentes; lo mismo ocurre con la aplicación y PostgreSQL.

Esto introduce contratos HTTP, nombres y direcciones, puertos, serialización, despliegue y fallos parciales. Ejecutar estas prácticas en un solo equipo permite aprender esos límites de comunicación. Todavía no hay una arquitectura completa de microservicios ni comunicación entre los tres módulos: son prácticas separadas.

RESTEasy implementa Jakarta REST y Undertow proporciona el servidor HTTP. **No equivalen a Core Profile y Web Profile**: esos perfiles agrupan especificaciones de Jakarta EE. Tomcat tampoco equivale a Core Profile. Los README de cada módulo amplían estas diferencias.

## Publicar cada módulo desde IntelliJ IDEA

Destino: [Ctrian/Programacion-Distribuida](https://github.com/Ctrian/Programacion-Distribuida).

### Situación local revisada

- La raíz Git es `laboratorio/`, no su carpeta padre `distribuida/`.
- El remoto `origin` apunta a `https://github.com/Ctrian/Programacion-Distribuida.git` tanto para fetch como para push.
- El commit existente ya contiene `web01` y `web02`; además hay archivos de `web03` parcialmente preparados en el índice.
- El repositorio de destino estaba vacío al revisarlo.

Un push envía commits y su historial alcanzable, no carpetas seleccionadas. Elegir solo un README para un nuevo commit no excluye los módulos que ya estaban en el historial anterior.

Se puede trabajar y publicar directamente desde `laboratorio/`, conservando su historial. GitHub mostrará el contenido de esa carpeta en la raíz del repositorio: `web01/`, `web02/`, `web03/` y los archivos compartidos de Gradle. No aparecerá una carpeta contenedora `laboratorio/`. Los módulos dependen del wrapper y de `settings.gradle.kts` de esa raíz; copiar solo sus directorios no conserva el proyecto Gradle completo.

Para publicar las prácticas progresivamente con un historial nuevo, el procedimiento siguiente utiliza una copia de trabajo del repositorio de destino. Los módulos quedarán como carpetas de **un mismo repositorio**, cada uno con su commit; no se necesitan repositorios Git anidados.

### 1. Abrir una copia del repositorio de destino

En IntelliJ, usar **Git → Clone** o **Get from Version Control** desde la pantalla inicial. Introducir la URL del destino y elegir una carpeta nueva, por ejemplo `Programacion-Distribuida`, fuera de `laboratorio/`.

Si pide autenticación, conectar la cuenta GitHub que tiene acceso al repositorio. Trabajar a partir de aquí en esa nueva carpeta.

### 2. Primer commit: configuración compartida y web01

Copiar desde `laboratorio/` a la nueva carpeta:

```text
.gitignore
build.gradle.kts
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
web01/build.gradle.kts
web01/README.md
web01/src/
```

Crear en el destino un `settings.gradle.kts` que inicialmente contenga:

```kotlin
rootProject.name = "Programacion-Distribuida"
include("web01")
```

Crear también un README raíz breve que enlace inicialmente solo a `web01`; se puede adaptar la introducción de este documento. Los enlaces a los otros módulos se añaden cuando se publiquen.

Copiar únicamente los archivos indicados: `.git/`, `.idea/`, `.gradle/`, `build/`, servidores descargados y `app-authors/` no forman parte de esta publicación. Mantener `gradle-wrapper.jar`: es parte del wrapper y está exceptuado en `.gitignore`.

Abrir o importar la nueva carpeta como proyecto Gradle. En **Commit**, revisar los archivos, incluir la configuración compartida y `web01`, y usar un mensaje como:

```text
feat(web01): agregar práctica WAR y documentación
```

Hacer **Commit** y después **Git → Push**. Revisar que el destino sea `Ctrian/Programacion-Distribuida` y la rama prevista (por ejemplo, `main`). Publicar un módulo incluye sus fuentes y configuración; subir únicamente el WAR no permite reconstruir la práctica.

### 3. Segundo commit: web02

Copiar `web02/build.gradle.kts`, `web02/README.md` y `web02/src/`. Añadir al `settings.gradle.kts` del destino:

```kotlin
include("web02")
```

Actualizar el README raíz con su enlace. Revisar esos cambios en **Commit**, usar `feat(web02): agregar RESTEasy con Undertow y documentación` y hacer **Commit** y **Push**.

### 4. Tercer commit: web03

Copiar `web03/build.gradle.kts`, `web03/README.md`, `web03/docker-compose.yml` y `web03/src/`. Añadir al `settings.gradle.kts` del destino:

```kotlin
include("web03")
```

Actualizar el README raíz. Revisar esos cambios, usar `feat(web03): agregar Helidon con PostgreSQL y documentación` y hacer **Commit** y **Push**. Los pendientes de ejecución están explicados en su README.

Cada publicación incluye los commits anteriores. El resultado final conserva las tres prácticas en carpetas independientes dentro del mismo repositorio, junto con los archivos Gradle compartidos.

### Un flujo parecido a VS Code

En **Settings → Version Control → Git → Enable staging area** se puede activar el área de preparación. Así se seleccionan explícitamente los archivos para cada commit. Sin esa opción, IntelliJ permite seleccionar los cambios desde su ventana **Commit**.

No es necesario cambiar de IDE ni instalar otro Git: commit y push tienen el mismo significado. La interfaz cambia cómo se seleccionan y revisan los archivos.

Referencia: [hacer commits y push en IntelliJ IDEA](https://www.jetbrains.com/help/idea/commit-and-push-changes.html).

## Correcciones aplicadas y estado de verificación

| Módulo | Corrección aplicada | Verificación realizada |
| --- | --- | --- |
| `web02` | Se declaró `application.mainClass` con `com.programacion.distribuida.web02.Web02Main`. | `:web02:installDist` generó correctamente el JAR, las dependencias y los lanzadores. |
| `web03` | El manifiesto pasó de la clase inexistente `org.example.web03.Application` a `com.programacion.distribuida.Web03Main`. | Se comprobó la clase incluida y el atributo `Main-Class` del JAR. |
| `web03` | Se permitió combinar todos los descriptores `META-INF/services/**` antes de excluir duplicados. | `:web03:shadowJar` terminó sin las advertencias de proveedores; se verificaron 16 descriptores con 20 proveedores y ninguna entrada ZIP duplicada. |
| `web03` | El usuario corrigió el mapeo: `.name(...)` lee `name` y `.title(...)` lee `title`. | Se revisó la corrección en el código fuente. |
| `web03` | `/books` ahora envía la lista consultada en JSON y cierra el stream de resultados. | JAR compilado y ejecutado temporalmente: HTTP `200`, tipo `application/json` y 8 libros de PostgreSQL con `name` y `title`, sin modificar datos. |

Las compilaciones se verificaron con el JDK completo instalado por IntelliJ en `/home/oli/.jdks/jbr-25.0.4.1`. La instalación Java seleccionada originalmente por la terminal no proporcionaba el compilador requerido. La ruta del JDK es específica de este equipo.

Los README explican la ejecución desde IntelliJ, mediante Gradle y mediante los artefactos generados. `web02` produce una distribución con `bin/` y `lib/`; `web03` produce un JAR con dependencias mediante Shadow. El usuario comunicó el despliegue de `web01` en Tomcat y TomEE, una respuesta satisfactoria de un endpoint y la creación de tablas y datos desde la interfaz de PostgreSQL. Los README distinguen esos pasos de la comprobación del saludo exacto de `web02` y de la nueva respuesta JSON de `web03`.

### Pendientes y mejoras

- **Base de datos reproducible:** la base local ya fue preparada por el usuario. Guardar SQL o migraciones permitiría reconstruirla en otro equipo; no es una corrección necesaria de los datos existentes.
- **Despliegue de `web01`:** el usuario confirmó Tomcat y TomEE. Si también funcionó el recurso REST, el entorno concreto ya queda validado para esa práctica; el HTML estático por sí solo no comprueba REST.
- **Pruebas de ejecución:** el usuario confirmó el saludo de `web02` en `/api/hola/Ana`. La nueva respuesta JSON de `web03` ya se comprobó contra PostgreSQL: devolvió 8 libros.
- **Reproducibilidad y mantenimiento:** fijar la versión de PostgreSQL, declarar persistencia explícita y revisar el aviso de características obsoletas antes de migrar a Gradle 10.

Se actualizaron los README, las configuraciones Gradle descritas y el manejador Java de `/books`. No se cambiaron dependencias ni se añadieron escrituras automáticas en la base. El remoto `origin` ya apunta al repositorio solicitado.

### Primera publicación preparada: web01

Se preparó una copia independiente en `../publicacion-web01/` que incluye solo `web01` y los archivos Gradle compartidos, con `include("web01")` en su configuración. Esto evita publicar los otros módulos del historial anterior. La copia tiene el commit `5fee957`, con el mensaje `feat(web01): agregar API REST y despliegue WAR con Gradle` y una descripción del endpoint, el despliegue y el modelo cliente-servidor. La generación del WAR se verificó correctamente.

El usuario completó la publicación de `web01` desde terminal con un token. Se verificó en GitHub que `main` contiene `5fee957`. El índice y el historial del proyecto original `laboratorio/` se conservaron.

### Segunda publicación preparada: web02

En la misma copia `../publicacion-web01/` se añadió `web02`, se incluyó en `settings.gradle.kts` y se amplió el README general. `:web02:installDist` compiló correctamente con JDK 25. El commit `91455bb`, `feat(web02): agregar RESTEasy y Undertow embebido`, está preparado sobre el commit publicado de `web01`.

La publicación automática de `web02` sigue pendiente: la terminal del agente no dispone de las credenciales utilizadas interactivamente por el usuario y la conexión GitHub rechaza escrituras con `403: Resource not accessible by integration`. Para completarla desde la terminal del usuario:

```bash
cd /home/oli/Documents/distribuida/publicacion-web01
git push -u origin main
```

No se necesita crear otro commit. Cuando Git solicite la contraseña por HTTPS, utilizar el token de acceso personal, sin compartirlo en el chat.
