plugins {
    java
    idea

    // Use Mojang mappings and a few other PaperAPI QOL.
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"

    // Automatic lombok and delombok configuration
    id("io.freefair.lombok") version "9.0.0-rc2"

    // Shade libraries into one "UberJar"
    id("com.gradleup.shadow") version "9.2.1"
}

// Used to configure which "path" we are compiling.
ext {
    println("Checking environment...")
    set("env", System.getProperty("env") ?: "release")
    set("isDev", get("env") == "dev")
    println("Found environment: " + get("env"))
    println("Dev Mode?: " + get("isDev"))
}

group = "com.itsschatten"
version = project.property("version")!! as String
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    // PaperMC.
    maven("https://repo.papermc.io/repository/maven-public/")

    // PlaceholderAPI.
    maven {
        url = uri("https://repo.extendedclip.com/releases/")
    }

    mavenCentral()
    mavenLocal()
}

dependencies {
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")

    implementation(platform("com.itsschatten:Yggdrasil-bom:1.0.2"))
    implementation("com.itsschatten:Yggdrasil") {
        isChanging = true
    }
    implementation("com.itsschatten:Yggdrasil-menus") {
        isChanging = true
    }
    implementation("com.itsschatten:Yggdrasil-anvilgui") {
        isChanging = true
    }

    compileOnly("com.itsschatten:Utilities-API:0.0.0-SNAPSHOT")

    implementation("org.apache.commons:commons-text:1.14.0")

    testImplementation(platform("org.junit:junit-bom:5.11.0-M1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    java {
        withSourcesJar()
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        if (ext.get("isDev") as Boolean) {
            archiveClassifier.set("dev")
            // Ignoring the version here allows us to replace an already existing jar in the server directory.
            archiveVersion.set("")
            if (project.property("dev-path").toString().isNotBlank()) {
                destinationDirectory.set(file(System.getProperty("user.home") + File.separator + project.property("dev-path")))
            }
        } else {
            archiveClassifier.set("")
            if (project.property("build-path").toString().isNotBlank()) {
                destinationDirectory.set(file(System.getProperty("user.home") + File.separator + project.property("build-path")))
            }
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        filteringCharset = "UTF-8"

        filesMatching("*plugin.yml") {
            expand("version" to project.property("version") as String)
        }
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}