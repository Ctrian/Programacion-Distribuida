# app-customers — Spring Boot y Consul

Módulo Gradle con Java 25, Spring Boot `4.2.0`, Spring Cloud `2026.0.0`, Spring MVC y Actuator. `CustomersAppMain` inicia el servidor HTTP. La ruta disponible para comprobar el arranque es `GET /actuator/health`. Los endpoints de clientes y la persistencia están pendientes. [Configuración de Gradle](build.gradle.kts) · [Clase de arranque](src/main/java/com/programacion/distribuida/customers/CustomersAppMain.java)

## Arranque con Consul

Desde la raíz del repositorio, con JDK 25 y un agente Consul disponible en `localhost:8500`, ejecutar:

```bash
bash gradlew :app-customers:bootRun
```

En IntelliJ IDEA también se puede ejecutar la clase `CustomersAppMain` del módulo `app-customers` con JDK 25. `application.yml` fija el puerto HTTP en `8090` y el nombre `app-customers`. Spring Cloud usa ese nombre y el puerto para generar el identificador de instancia `app-customers-8090` con la configuración predeterminada. Si aparece el error de identificador `null`, comprobar que `application.yml` esté en el classpath del módulo y que `spring.application.name` tenga valor. [Configuración de Spring Boot](src/main/resources/application.yml)

Desde otra terminal, consultar el estado del servidor y buscar `app-customers` en los registros del agente.

```bash
curl -fsS http://localhost:8090/actuator/health
curl -fsS http://localhost:8500/v1/agent/services
```

Si el agente no está disponible, el registro automático hace fallar el arranque. Para iniciar únicamente el servidor local, desactivar la integración con Consul en esa ejecución.

```bash
bash gradlew :app-customers:bootRun --args='--spring.cloud.consul.enabled=false'
```

## Empaquetar

```bash
bash gradlew :app-customers:bootJar
java -jar app-customers/build/libs/app-customers-0.0.1-SNAPSHOT.jar
```
