plugins {
    id("java")

    // Use Mojang mappings and a few other PaperAPI QOL.
    id("io.papermc.paperweight.userdev") version "1.7.1"
    // Automatic lombok and delombok configuration
    id("io.freefair.lombok") version "8.6"

    // Shade libraries into one "UberJar"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.itsschatten"
version = project.property("version") as String
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    // PaperMC.
    maven("https://repo.papermc.io/repository/maven-public/")

    // PlaceholderAPI.
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    mavenCentral()
    mavenLocal()
}

dependencies {
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")

    implementation("com.itsschatten:Yggdrasil-Paper:2.0.4") {
        isChanging = true
    }

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    testImplementation(platform("org.junit:junit-bom:5.11.0-M1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    reobfJar {
        outputJar.set(file(System.getProperty("user.home") + File.separator + project.property("dev-path") + File.separator + project.name + "-dev-build.jar"))
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand("version" to project.property("version") as String)
        }
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}