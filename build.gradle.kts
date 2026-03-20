import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

plugins {
    id("idea")
    id("com.gradleup.shadow") version "9.3.1"
    id("java")
    id("application")
    id("org.panteleyev.jpackageplugin") version "2.0.0"
}

group = "net.paulem.launchermc"
version = "1.3.0"

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
    implementation("fr.flowarg:flowupdater:1.9.4")
    implementation("fr.flowarg:openlauncherlib:3.2.11")
    implementation("org.kohsuke:github-api:2.0.0-alpha-2")
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

    input = layout.buildDirectory.dir("libs")
    mainJar = tasks.shadowJar.get().archiveFileName.get()
    mainClass = application.mainClass.get()
    javaOptions = listOf("-Dfile.encoding=UTF-8", "--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED")
}

val currentOs = OperatingSystem.current()
val osName = when {
    currentOs.isWindows -> "windows"
    currentOs.isMacOsX -> "macos"
    currentOs.isLinux -> "linux"
    else -> "unknown"
}

// Chaque tâche a maintenant son dossier cible isolé
val packageMsi = tasks.register<JPackageTask>("packageMsi") {
    type = ImageType.MSI
    destination = layout.buildDirectory.dir("dist/msi")
    windows { icon = layout.projectDirectory.file("icons/icons.ico"); winConsole = true; winMenu = true; winDirChooser = true; winPerUserInstall = true; winShortcut = true }
}

val packageExe = tasks.register<JPackageTask>("packageExe") {
    type = ImageType.EXE
    destination = layout.buildDirectory.dir("dist/exe")
    windows { icon = layout.projectDirectory.file("icons/icons.ico"); winConsole = true; winMenu = true; winDirChooser = true; winPerUserInstall = true; winShortcut = true }
}

val packageDeb = tasks.register<JPackageTask>("packageDeb") {
    type = ImageType.DEB
    destination = layout.buildDirectory.dir("dist/deb")
}

val packageRpm = tasks.register<JPackageTask>("packageRpm") {
    type = ImageType.RPM
    destination = layout.buildDirectory.dir("dist/rpm")
}

val packageDmg = tasks.register<JPackageTask>("packageDmg") {
    type = ImageType.DMG
    destination = layout.buildDirectory.dir("dist/dmg")
    mac { icon = layout.projectDirectory.file("icons/icons.icns") }
}

val packagePkg = tasks.register<JPackageTask>("packagePkg") {
    type = ImageType.PKG
    destination = layout.buildDirectory.dir("dist/pkg")
    mac { icon = layout.projectDirectory.file("icons/icons.icns") }
}

val zipjpackage = tasks.register<JPackageTask>("zipjpackage") {
    type = ImageType.APP_IMAGE
    destination = layout.buildDirectory.dir("dist/appimage")
    mac { icon = layout.projectDirectory.file("icons/icons.icns") }
    windows { icon = layout.projectDirectory.file("icons/icons.ico") }
}

tasks.register<Zip>("zipPackage") {
    dependsOn(zipjpackage)

    archiveFileName.set("$osName-${project.name}-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist/zip"))

    // On compresse le contenu du sous-dossier généré par zipjpackage
    from(layout.buildDirectory.dir("dist/appimage/${project.name}"))
}

tasks.jar {
    finalizedBy(tasks.shadowJar)
    manifest { attributes("Implementation-Version" to project.version) }
}

tasks.shadowJar {
    minimize()
    archiveVersion.set("")
    archiveClassifier.set("")
    mustRunAfter(tasks.distZip, tasks.distTar, tasks.startScripts)
}