plugins {
    id("java")
}

group = "com.itsschatten"
version = project.property("version").toString()

repositories {
    mavenCentral()

    // PaperMC.
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

java {
    withSourcesJar()
    withJavadocJar()
}