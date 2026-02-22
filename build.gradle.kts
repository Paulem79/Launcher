import org.gradle.internal.jvm.Jvm
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask
import java.security.MessageDigest

plugins {
    id("idea")
    id("com.gradleup.shadow") version "9.0.0"
    id("java")
    id("application")
    id("org.panteleyev.jpackageplugin") version "1.7.6"
}

group = "net.paulem.launchermc"
version = "1.1.0"

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://litarvan.github.io/maven") }
    maven { url = uri("https://maven.paulem.net/releases") }
    maven("https://repo.jenkins-ci.org/public/")
}

dependencies {
    implementation("fr.litarvan:openauth:1.+")
    implementation("fr.flowarg:materialdesignfontfx:7.+")
    implementation("fr.flowarg:flowupdater:1.9.3")
    implementation("fr.flowarg:openlauncherlib:3.2.11")
    implementation("org.kohsuke:github-api:2.0-rc.5")
    implementation("club.minnced:java-discord-rpc:2.0.3")
    implementation("io.github.typhon0:AnimateFX:1.3.0")
    implementation("com.google.code.gson:gson:2.+")
    implementation("org.jetbrains:annotations:26.+")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

application {
    mainClass.set("$group.Main")
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

// --- CONFIGURATION COMMUNE JPACKAGE ---
tasks.withType<JPackageTask>().configureEach {
    dependsOn(tasks.shadowJar)
    appName = project.name
    appVersion = project.version.toString()
    vendor = "Paulem"
    copyright = "Copyright (c) 2025 Paulem"
    runtimeImage = Jvm.current().javaHome
    destination = layout.projectDirectory.dir("dist")
    input = layout.buildDirectory.dir("libs")
    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = application.mainClass.get()
    javaOptions = listOf("-Dfile.encoding=UTF-8", "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED")
}

// Tâches spécifiques par format
tasks.register<JPackageTask>("packageMsi") { windows { type = ImageType.MSI; icon = layout.projectDirectory.file("icons/icons.ico"); winConsole = true; winMenu = true; winDirChooser = true; winPerUserInstall = true; winShortcut = true } }
tasks.register<JPackageTask>("packageExe") { windows { type = ImageType.EXE; icon = layout.projectDirectory.file("icons/icons.ico"); winConsole = true; winMenu = true; winDirChooser = true; winPerUserInstall = true; winShortcut = true } }
tasks.register<JPackageTask>("packageDeb") { linux { type = ImageType.DEB } }
tasks.register<JPackageTask>("packageRpm") { linux { type = ImageType.RPM } }
tasks.register<JPackageTask>("packageDmg") { mac { type = ImageType.DMG; icon = layout.projectDirectory.file("icons/icons.icns") } }
tasks.register<JPackageTask>("packagePkg") { mac { type = ImageType.PKG; icon = layout.projectDirectory.file("icons/icons.icns") } }

var infra = ""
tasks.register<JPackageTask>("zipjpackage") {
    type = ImageType.APP_IMAGE
    finalizedBy("zipPackage")
    linux { infra = "linux" }
    mac { infra = "macos"; icon = layout.projectDirectory.file("icons/icons.icns") }
    windows { infra = "windows"; icon = layout.projectDirectory.file("icons/icons.ico") }
}

tasks.register<Zip>("zipPackage") {
    archiveFileName.set("$infra-${project.name}-${project.version}.zip")
    destinationDirectory.set(layout.projectDirectory.dir("dist"))
    from(layout.projectDirectory.dir("dist/${project.name}"))
}

tasks.register("generateChecksums") {
    group = "distribution"
    doLast {
        val distDir = layout.projectDirectory.dir("dist").asFile
        distDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".sha256") }?.forEach { file ->
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = file.readBytes().let { bytes ->
                digest.digest(bytes).joinToString("") { "%02x".format(it) }
            }
            File(file.absolutePath + ".sha256").writeText(hash)
        }
    }
}

tasks.jar {
    finalizedBy(tasks.shadowJar)

    manifest {
        attributes("Implementation-Version" to project.version)
    }
}

tasks.shadowJar {
    minimize()
    archiveVersion.set("")
    archiveClassifier.set("")

    mustRunAfter(tasks.distZip)
    mustRunAfter(tasks.distTar)
    mustRunAfter(tasks.startScripts)
}