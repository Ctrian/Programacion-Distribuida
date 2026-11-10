plugins {
    id("java")
    id("io.freefair.lombok") version "9.8.3"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val helidonVersion = "4.5.6";

dependencies {
    // Source: https://mvnrepository.com/artifact/io.helidon.webserver/helidon-webserver
    // servidor http
    implementation("io.helidon.webserver:helidon-webserver:$helidonVersion")

    implementation("io.helidon.http.media:helidon-http-media-jsonp:$helidonVersion")
    implementation("io.helidon.http.media:helidon-http-media-jsonb:$helidonVersion")

    //configuracion
    implementation("io.helidon.config:helidon-config-yaml:${helidonVersion}")

    // acceso a la db
    implementation("io.helidon.dbclient:helidon-dbclient:${helidonVersion}")
    implementation("io.helidon.dbclient:helidon-dbclient-jdbc:${helidonVersion}")
    implementation("io.helidon.dbclient:helidon-dbclient-hikari:${helidonVersion}")
    // Source: https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.7.14")

}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Entrega todos los registros de proveedores al combinador antes de descartar duplicados.
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
}

// Define la entrada de la aplicación; Shadow hereda este manifiesto.
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.programacion.distribuida.Web03Main"
    }
}

