# web01 — Aplicación REST empaquetada como WAR

Primera práctica: separar la aplicación del servidor que la ejecuta. Gradle genera un archivo `.war` que se despliega en un servidor externo.

## Qué contiene

- `MiAplicacionRest`: establece `/api` como base de la API con `@ApplicationPath`.
- `HolaRest`: atiende `GET /hola/{name}` y devuelve un saludo con la fecha y hora del servidor.
- `src/main/webapp/index.html`: página estática con el texto «Hola mundo».
- `build.gradle.kts`: aplica los plugins `java` y `war`.

La dependencia `jakarta.ws.rs-api:4.0.0` está declarada como `compileOnly`: permite compilar las anotaciones REST, pero no incorpora al WAR una implementación que atienda esas peticiones. El entorno de despliegue debe proporcionarla.

## Tomcat, TomEE y los perfiles

Tomcat es un servidor HTTP y contenedor de servlets. Por sí solo no implementa Jakarta REST ni equivale a Jakarta EE Core Profile. Poder desplegar un WAR no significa que estén disponibles todas las tecnologías utilizadas dentro de él.

TomEE añade tecnologías empresariales sobre Tomcat y ofrece distintas distribuciones. Core Profile y Web Profile son conjuntos de especificaciones de Jakarta EE, no nombres para el «núcleo» y el «servidor web» de una aplicación.

En el entorno local hay Tomcat 9 y TomEE 11 WebProfile. **Este código usa `jakarta.ws.rs` 4.0 y el WAR no incluye su implementación**: no debe darse por hecho que funcionará la API al copiarlo a Tomcat 9. Ese Tomcat usa la generación `javax.servlet`; integrar Jakarta REST 4.0 requiere un entorno de servlet compatible y una implementación REST. Si se utiliza TomEE, hay que verificar las capacidades concretas de su versión y distribución.

## Compilación y despliegue

Abrir la raíz del proyecto Gradle en IntelliJ IDEA: `laboratorio/` en el entorno local o la carpeta del repositorio tras clonarlo. Todos los comandos de esta sección se ejecutan desde esa raíz, donde están `gradlew` y `settings.gradle.kts`:

```bash
bash gradlew :web01:war
```

El resultado esperado es `web01/build/libs/web01-1.0-SNAPSHOT.war`. El wrapper se invoca con `bash` porque el archivo local no tiene permiso de ejecución.

Copiar el WAR al directorio `webapps/` del servidor compatible configurado para la práctica y arrancarlo. Si se conserva ese nombre de archivo y se usa el puerto 8080:

```bash
curl http://localhost:8080/web01-1.0-SNAPSHOT/
curl http://localhost:8080/web01-1.0-SNAPSHOT/api/hola/Ana
```

La primera dirección sirve la página estática. La segunda requiere que Jakarta REST esté integrado correctamente. El contexto cambia si se renombra el WAR o se configura otro contexto en IntelliJ/servidor.

**Verificación para la publicación:** `:web01:war` compiló correctamente en una copia que incluye únicamente este módulo y la configuración Gradle compartida, usando un JDK 25 completo. La compilación comprueba la generación del WAR; la ejecución del recurso REST depende del servidor configurado.

## Relación con programación distribuida

El usuario confirmó que realizó el despliegue en Tomcat y TomEE. Si además se verificó el saludo de `/api/hola/{name}` bajo el contexto del WAR, esa ejecución confirma que el entorno concreto proporciona la integración REST necesaria. Cargar solo `index.html` o ver el WAR desplegado confirma el alojamiento web, pero no comprueba el recurso REST. Las diferencias entre productos descritas arriba explican sus capacidades por defecto y no invalidan una instalación que haya sido configurada con componentes adicionales.

```text
Cliente (navegador/curl) -- HTTP --> Servidor externo -- invocación --> Recurso REST
```

El cliente y el servidor pueden ejecutarse en equipos distintos y comunicarse mediante HTTP. La práctica introduce direcciones, puertos, contratos de petición/respuesta y despliegue remoto. Es una base cliente-servidor; todavía no hay comunicación entre varios servicios propios.

## Referencias

- [Tecnologías y versiones de Tomcat](https://tomcat.apache.org/whichversion.html).
- [Distribuciones de TomEE](https://tomee.apache.org/latest/docs/comparison.html).
- [Jakarta EE Core Profile](https://jakarta.ee/specifications/coreprofile/11/) y [Web Profile](https://jakarta.ee/specifications/webprofile/11/).
