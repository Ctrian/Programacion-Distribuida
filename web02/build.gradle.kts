plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jboss.resteasy:resteasy-core:7.0.5.Final")
    implementation("org.jboss.resteasy:resteasy-undertow:7.0.5.Final")
}
