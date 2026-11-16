plugins {
    id("java")
    id("io.quarkus") version "3.40.1.1"
    id("io.freefair.lombok") version "9.8.3"

}

group = "com.programacion.distribuida"
version = "unspecified"

repositories {
    mavenCentral()
}

val quarkusVersion = "3.40.1.1"


java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}
dependencies {

    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    //CDI
    implementation("io.quarkus:quarkus-arc")
    //REST
    implementation("io.quarkus:quarkus-rest")
    //JSON B
    implementation("io.quarkus:quarkus-rest-jsonb")

}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}