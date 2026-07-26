package net.paulem.launchermc.game;

import net.paulem.launchermc.Launcher;
import net.paulem.launchermc.game.minecraft.MinecraftInfos;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixes missing Forge processor JARs after FlowUpdater completes.
 *
 * Mojang sometimes re-releases the Minecraft client.jar with security patches,
 * which changes its SHA hash. The Forge installer's internal hash verification
 * then rejects the jarsplitter output, deletes the generated slim/extra/srg/forge-client
 * JARs, and reports "Processor failed, invalid outputs".
 *
 * This class detects the missing JARs and re-runs the Forge processing steps
 * (jarsplitter, ForgeAutoRenamingTool, binarypatcher) manually, bypassing the
 * hash verification that the Forge installer performs, so the game can launch.
 */
public class ForgeFixer {

    private static final String MC_VERSION = MinecraftInfos.GAME_VERSION;
    private static final String FORGE_VERSION = MinecraftInfos.MODLOADER_VERSION;
    private static final String FORGE_FULL_VERSION = MinecraftInfos.MODLOADER_VERSION;

    private final Path launcherDir;

    public ForgeFixer(Path launcherDir) {
        this.launcherDir = launcherDir;
    }

    // ── File path helpers ──────────────────────────────────────────────

    private Path lib(String first, String... rest) {
        return launcherDir.resolve("libraries").resolve(Path.of(first, rest));
    }

    private Path clientDir() {
        return lib("net/minecraft/client", MC_VERSION + "-" + MinecraftInfos.MCP_VERSION);
    }

    private Path slimJar()   { return clientDir().resolve("client-" + MC_VERSION + "-" + MinecraftInfos.MCP_VERSION + "-slim.jar"); }
    private Path extraJar()  { return clientDir().resolve("client-" + MC_VERSION + "-" + MinecraftInfos.MCP_VERSION + "-extra.jar"); }
    private Path srgJar()    { return clientDir().resolve("client-" + MC_VERSION + "-" + MinecraftInfos.MCP_VERSION + "-srg.jar"); }

    private Path forgeClientJar() {
        return lib("net/minecraftforge/forge", FORGE_FULL_VERSION, "forge-" + FORGE_FULL_VERSION + "-client.jar");
    }

    private Path mergedMappings() {
        return lib("de/oceanlabs/mcp/mcp_config", MC_VERSION + "-" + MinecraftInfos.MCP_VERSION,
                "mcp_config-" + MC_VERSION + "-" + MinecraftInfos.MCP_VERSION + "-mappings-merged.txt");
    }

    private Path mojmapMappings() {
        return clientDir().resolve("client-" + MC_VERSION + "-" + MinecraftInfos.MCP_VERSION + "-mappings.txt");
    }

    private Path abs(Path p) { return p.toAbsolutePath().normalize(); }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Returns true if at least one of the critical Forge processor output JARs
     * is missing and needs to be recreated. Only checks if Forge is actually installed.
     */
    public boolean needsFix() {
        if(MinecraftInfos.MCP_VERSION == null) return false;

        // Only attempt a fix if Forge is actually installed (version JSON exists)
        Path forgeJson = launcherDir.resolve(MC_VERSION + "-forge-" + FORGE_VERSION.split("-")[1] + ".json");
        if (Files.notExists(forgeJson)) return false;

        // Also verify the client directory exists (validates it's a Forge " + MC_VERSION + " install)
        if (Files.notExists(clientDir())) return false;

        return Files.notExists(slimJar()) || Files.notExists(extraJar()) || Files.notExists(srgJar()) || Files.notExists(forgeClientJar());
    }

    /**
     * Re-runs the Forge processor pipeline to recreate missing JARs.
     * Call AFTER FlowUpdater.update() has completed.
     *
     * @return true if all JARs were successfully created, false otherwise
     */
    public boolean fix() {
        Launcher.getInstance().getLogger().info("ForgeFixer: checking for missing processor JARs...");

        if (!needsFix()) {
            Launcher.getInstance().getLogger().info("ForgeFixer: all JARs present, nothing to do.");
            return true;
        }

        Launcher.getInstance().getLogger().info("ForgeFixer: missing JARs detected, re-running Forge processors...");

        try {
            // Step 0: ensure we have the binpatch extracted
            ensureBinpatch();

            // Step 1: create versions/1.20.1/1.20.1.jar (the jarsplitter input)
            Path versionsJar = launcherDir.resolve("versions").resolve(MC_VERSION).resolve(MC_VERSION + ".jar");
            Path clientJar = launcherDir.resolve("client.jar");
            Path versionsDir = versionsJar.getParent();

            if (Files.notExists(versionsDir)) {
                Files.createDirectories(versionsDir);
            }
            if (Files.notExists(versionsJar) || Files.size(versionsJar) != Files.size(clientJar)) {
                Files.copy(clientJar, versionsJar, StandardCopyOption.REPLACE_EXISTING);
                Launcher.getInstance().getLogger().info("ForgeFixer: copied client.jar to " + versionsJar);
            }

            // Step 2: run jarsplitter → slim.jar + extra.jar
            if (Files.notExists(slimJar()) || Files.notExists(extraJar())) {
                if (!runJarSplitter(versionsJar)) {
                    Launcher.getInstance().getLogger().err("ForgeFixer: jarsplitter failed");
                    return false;
                }
                Launcher.getInstance().getLogger().info("ForgeFixer: jarsplitter created slim.jar + extra.jar");
            } else {
                Launcher.getInstance().getLogger().info("ForgeFixer: slim.jar + extra.jar already exist, skipping jarsplitter");
            }

            // Step 3: run ForgeAutoRenamingTool → srg.jar
            if (Files.notExists(srgJar())) {
                if (!runForgeAutoRenamingTool()) {
                    Launcher.getInstance().getLogger().err("ForgeFixer: ForgeAutoRenamingTool failed");
                    return false;
                }
                Launcher.getInstance().getLogger().info("ForgeFixer: ForgeAutoRenamingTool created srg.jar");
            } else {
                Launcher.getInstance().getLogger().info("ForgeFixer: srg.jar already exists, skipping renaming");
            }

            // Step 4: run binarypatcher → forge-client.jar
            if (Files.notExists(forgeClientJar())) {
                if (!runBinaryPatcher()) {
                    Launcher.getInstance().getLogger().err("ForgeFixer: binarypatcher failed");
                    return false;
                }
                Launcher.getInstance().getLogger().info("ForgeFixer: binarypatcher created forge-client.jar");
            } else {
                Launcher.getInstance().getLogger().info("ForgeFixer: forge-client.jar already exists, skipping patcher");
            }

            // Clean up: remove the temporary versions dir
            try {
                deleteDirectory(versionsDir);
            } catch (Exception ignored) {}

            boolean success = !needsFix();
            if (success) {
                Launcher.getInstance().getLogger().info("ForgeFixer: all JARs created successfully!");
            } else {
                Launcher.getInstance().getLogger().err("ForgeFixer: some JARs are still missing");
            }
            return success;

        } catch (Exception e) {
            Launcher.getInstance().getLogger().err("ForgeFixer: error during fix: " + e.getMessage());
            Launcher.getInstance().getLogger().printStackTrace(e);
            return false;
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────

    private void ensureBinpatch() throws IOException, InterruptedException {
        Path dataDir = launcherDir.resolve("data");
        Path lzma = dataDir.resolve("client.lzma");

        if (Files.notExists(lzma)) {
            Files.createDirectories(dataDir);
            // The Forge installer jar was downloaded. Re-download it to extract the lzma
            String installerUrl = "https://maven.minecraftforge.net/net/minecraftforge/forge/"
                    + FORGE_FULL_VERSION + "/forge-" + FORGE_FULL_VERSION + "-installer.jar";

            // Instead of re-downloading, use the one we already have
            // It was saved at /tmp/forge-installer.jar earlier
            Path tmpInstaller = Path.of("/tmp/forge-installer.jar");
            if (Files.exists(tmpInstaller)) {
                // Extract client.lzma from the installer jar
                ProcessBuilder pb = new ProcessBuilder(
                        "unzip", "-o", tmpInstaller.toAbsolutePath().toString(),
                        "data/client.lzma", "-d", launcherDir.toAbsolutePath().toString()
                );
                pb.inheritIO();
                int exit = pb.start().waitFor();
                if (exit != 0) {
                    throw new IOException("Failed to extract client.lzma from installer jar, exit=" + exit);
                }
                Launcher.getInstance().getLogger().info("ForgeFixer: extracted client.lzma from installer");
            } else {
                // Download it
                try (var in = new java.net.URL(installerUrl).openStream()) {
                    Path tmp = launcherDir.resolve("forge-installer-tmp.jar");
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);

                    ProcessBuilder pb = new ProcessBuilder(
                            "unzip", "-o", tmp.toAbsolutePath().toString(),
                            "data/client.lzma", "-d", launcherDir.toAbsolutePath().toString()
                    );
                    pb.inheritIO();
                    int exit = pb.start().waitFor();
                    Files.deleteIfExists(tmp);
                    if (exit != 0) {
                        throw new IOException("Failed to extract client.lzma, exit=" + exit);
                    }
                    Launcher.getInstance().getLogger().info("ForgeFixer: downloaded installer & extracted client.lzma");
                }
            }
        }
    }

    /**
     * Runs jarsplitter to split the client JAR into slim and extra parts.
     */
    private boolean runJarSplitter(Path versionsJar) {
        try {
            String sep = File.pathSeparator;
            String libsDir = launcherDir.resolve("libraries").toAbsolutePath().normalize().toString();

            // Build classpath with platform-independent paths
            String cp = String.join(sep,
                    Path.of(libsDir, "net/minecraftforge/jarsplitter/1.1.4/jarsplitter-1.1.4.jar").toString(),
                    Path.of(libsDir, "net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar").toString(),
                    Path.of(libsDir, "net/minecraftforge/srgutils/0.4.3/srgutils-0.4.3.jar").toString()
            );

            List<String> cmd = new ArrayList<>();
            cmd.add(findJava());
            cmd.add("-cp");
            cmd.add(cp);
            cmd.add("net.minecraftforge.jarsplitter.ConsoleTool");
            cmd.add("--input");
            cmd.add(abs(versionsJar).toString());
            cmd.add("--slim");
            cmd.add(abs(slimJar()).toString());
            cmd.add("--extra");
            cmd.add(abs(extraJar()).toString());
            cmd.add("--srg");
            cmd.add(abs(mergedMappings()).toString());

            return runProcess(cmd);
        } catch (Exception e) {
            Launcher.getInstance().getLogger().err("ForgeFixer: jarsplitter exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Runs ForgeAutoRenamingTool (FART) to apply SRG naming to slim.jar → srg.jar.
     */
    private boolean runForgeAutoRenamingTool() {
        try {
            String sep = File.pathSeparator;
            String libsDir = launcherDir.resolve("libraries").toAbsolutePath().normalize().toString();

            String cp = String.join(sep,
                    Path.of(libsDir, "net/minecraftforge/ForgeAutoRenamingTool/0.1.22/ForgeAutoRenamingTool-0.1.22-all.jar").toString(),
                    Path.of(libsDir, "net/sf/jopt-simple/jopt-simple/6.0-alpha-3/jopt-simple-6.0-alpha-3.jar").toString(),
                    Path.of(libsDir, "org/ow2/asm/asm-commons/9.2/asm-commons-9.2.jar").toString(),
                    Path.of(libsDir, "org/ow2/asm/asm-analysis/9.2/asm-analysis-9.2.jar").toString(),
                    Path.of(libsDir, "org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar").toString(),
                    Path.of(libsDir, "org/ow2/asm/asm/9.2/asm-9.2.jar").toString(),
                    Path.of(libsDir, "net/minecraftforge/srgutils/0.4.9/srgutils-0.4.9.jar").toString()
            );

            List<String> cmd = new ArrayList<>();
            cmd.add(findJava());
            cmd.add("-cp");
            cmd.add(cp);
            cmd.add("net.minecraftforge.fart.Main");
            cmd.add("--input");
            cmd.add(abs(slimJar()).toString());
            cmd.add("--output");
            cmd.add(abs(srgJar()).toString());
            cmd.add("--names");
            cmd.add(abs(mergedMappings()).toString());
            cmd.add("--ann-fix");
            cmd.add("--ids-fix");
            cmd.add("--src-fix");
            cmd.add("--record-fix");

            return runProcess(cmd);
        } catch (Exception e) {
            Launcher.getInstance().getLogger().err("ForgeFixer: FART exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Runs binarypatcher to apply Forge patches, creating forge-client.jar.
     */
    private boolean runBinaryPatcher() {
        try {
            String sep = File.pathSeparator;
            String libsDir = launcherDir.resolve("libraries").toAbsolutePath().normalize().toString();
            String lzmaPath = launcherDir.resolve("data/client.lzma").toAbsolutePath().normalize().toString();

            String cp = String.join(sep,
                    Path.of(libsDir, "net/minecraftforge/binarypatcher/1.1.1/binarypatcher-1.1.1.jar").toString(),
                    Path.of(libsDir, "commons-io/commons-io/2.4/commons-io-2.4.jar").toString(),
                    Path.of(libsDir, "com/google/guava/guava/25.1-jre/guava-25.1-jre.jar").toString(),
                    Path.of(libsDir, "net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar").toString(),
                    Path.of(libsDir, "com/github/jponge/lzma-java/1.3/lzma-java-1.3.jar").toString(),
                    Path.of(libsDir, "com/nothome/javaxdelta/2.0.1/javaxdelta-2.0.1.jar").toString(),
                    Path.of(libsDir, "com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar").toString(),
                    Path.of(libsDir, "org/checkerframework/checker-qual/2.0.0/checker-qual-2.0.0.jar").toString(),
                    Path.of(libsDir, "com/google/errorprone/error_prone_annotations/2.1.3/error_prone_annotations-2.1.3.jar").toString(),
                    Path.of(libsDir, "com/google/j2objc/j2objc-annotations/1.1/j2objc-annotations-1.1.jar").toString(),
                    Path.of(libsDir, "org/codehaus/mojo/animal-sniffer-annotations/1.14/animal-sniffer-annotations-1.14.jar").toString(),
                    Path.of(libsDir, "trove/trove/1.0.2/trove-1.0.2.jar").toString()
            );

            List<String> cmd = new ArrayList<>();
            cmd.add(findJava());
            cmd.add("-cp");
            cmd.add(cp);
            cmd.add("net.minecraftforge.binarypatcher.ConsoleTool");
            cmd.add("--clean");
            cmd.add(abs(srgJar()).toString());
            cmd.add("--output");
            cmd.add(abs(forgeClientJar()).toString());
            cmd.add("--apply");
            cmd.add(lzmaPath);

            return runProcess(cmd);
        } catch (Exception e) {
            Launcher.getInstance().getLogger().err("ForgeFixer: binarypatcher exception: " + e.getMessage());
            return false;
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────

    private String findJava() {
        // Try to find the Java executable (same as the one running the launcher)
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path javabin = Path.of(javaHome, "bin", "java");
            if (Files.exists(javabin)) return javabin.toAbsolutePath().normalize().toString();
            javabin = Path.of(javaHome, "bin", "java.exe");
            if (Files.exists(javabin)) return javabin.toAbsolutePath().normalize().toString();
        }
        return "java";
    }

    private boolean runProcess(List<String> cmd) throws IOException, InterruptedException {
        Launcher.getInstance().getLogger().info("ForgeFixer: running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(launcherDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Read all output inline before waitFor() to prevent stdout buffer deadlock.
        // readLine() blocks for new output, returns null when the stream closes (process exited).
        try (var is = p.getInputStream();
             var br = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                Launcher.getInstance().getLogger().info("[ForgeProc] " + line);
            }
        }

        int exitCode = p.waitFor();

        if (exitCode != 0) {
            Launcher.getInstance().getLogger().err("ForgeFixer: process exited with code " + exitCode);
            return false;
        }
        return true;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            }
        }
    }
}
