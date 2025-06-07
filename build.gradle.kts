import org.gradle.internal.jvm.Jvm
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

plugins {
    id("idea")
    id("com.gradleup.shadow") version "8.+"
    id("java")
    id("application")
    id("org.panteleyev.jpackageplugin") version "1.7.0"
}

group = "ovh.paulem.launchermc"
version = "1.0.7"

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://jitpack.io")
        name = "JitPack"
    }
    maven {
        url = uri("https://litarvan.github.io/maven")
        name = "LitarvanMaven"
    }
    maven {
        url = uri("https://maven.scijava.org/content/repositories/public/")
        name = "ClubMinnced"
    }
    maven {
        name = "paulemReleases"
        url = uri("https://maven.paulem.ovh/releases")
    }
}

dependencies {
    implementation("fr.litarvan:openauth:1.+")
    implementation("fr.flowarg:materialdesignfontfx:7.+")

    implementation("fr.flowarg:flowupdater:1.9.2")
    implementation("fr.flowarg:openlauncherlib:3.2.11")

    implementation("club.minnced:java-discord-rpc:2.0.2")

    implementation("com.google.code.gson:gson:2.+")
    implementation("org.jetbrains:annotations:26.+")
}

application {
    mainClass.set("ovh.paulem.launchermc.Main")
}

val javaVersion = JavaLanguageVersion.of(23)

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = javaVersion.toString()
    targetCompatibility = javaVersion.toString()
    options.encoding = "UTF-8"
}

java {
    toolchain {
        languageVersion = javaVersion
        vendor = JvmVendorSpec.BELLSOFT
    }
}

tasks.withType<JPackageTask>().configureEach {
    dependsOn(tasks.shadowJar)

    appName = project.name
    appVersion = project.version.toString()
    vendor = "Paulem"
    copyright = "Copyright (c) 2025 Paulem"
    runtimeImage = Jvm.current().javaHome.toString()
    destination = "dist"
    removeDestination = true
    input = "build/libs"
    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = application.mainClass.get()
    javaOptions = listOf("-Dfile.encoding=UTF-8", "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED")
}

var infra = ""
tasks.register<JPackageTask>("zipjpackage") {
    finalizedBy("zipPackage")

    type = ImageType.APP_IMAGE

    linux {
        infra = "linux"
    }

    mac {
        icon = "icons/icons.icns"
        infra = "macos"
    }

    windows {
        icon = "icons/icons.ico"

        winConsole = true
        infra = "windows"
    }
}

tasks.register<Zip>("zipPackage") {
    archiveFileName.set(infra + "-"  + project.name + "-" + project.version + ".zip")
    destinationDirectory.set(layout.projectDirectory.dir("dist"))

    from(layout.projectDirectory.dir("dist/" + project.name))
}

tasks.jpackage {
    linux {
        type = ImageType.DEB
    }

    mac {
        icon = "icons/icons.icns"

        type = ImageType.DMG
    }

    windows {
        icon = "icons/icons.ico"

        type = ImageType.MSI

        winConsole = true
        if(type == ImageType.EXE || type == ImageType.MSI) {
            winMenu = true
            winDirChooser = true
            winPerUserInstall = true
            winShortcut = true
            winShortcutPrompt = true
            // winUpdateUrl can be interesting for auto-updates
        }
    }
}

tasks.jar {
    finalizedBy(tasks.shadowJar)
}

tasks.shadowJar {
    mustRunAfter(tasks.distZip)
    mustRunAfter(tasks.distTar)
    mustRunAfter(tasks.startScripts)

    archiveVersion.set("")
    archiveClassifier.set("")
}

tasks.register<JavaExec>("runShadowJar") {
    val javaPath = Jvm.current().javaExecutable.toString()

    group = "application"
    description = "Builds and runs the shadow jar using the specified Java path"

    dependsOn(tasks.shadowJar)

    classpath = files(tasks.shadowJar.get().archiveFile)
    setExecutable(javaPath)
    jvmArgs("--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED")

    finalizedBy(tasks.clean)
}