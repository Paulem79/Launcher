package net.paulem.launchermc.updater;

import net.paulem.launchermc.Launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Installs a downloaded update. A small script is started detached: it waits for the launcher to exit,
 * runs the platform installer, then starts the launcher again. The user only has to confirm once.
 */
public final class UpdateInstaller {
    public enum OperatingSystem {WINDOWS, MAC, LINUX, OTHER}

    public static final OperatingSystem OS = detectOs();

    private UpdateInstaller() {
    }

    private static OperatingSystem detectOs() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) return OperatingSystem.WINDOWS;
        if (os.contains("mac")) return OperatingSystem.MAC;
        if (os.contains("nux")) return OperatingSystem.LINUX;
        return OperatingSystem.OTHER;
    }

    public static boolean isDebianBased() {
        return hasCommand("dpkg");
    }

    public static boolean isRpmBased() {
        return hasCommand("rpm");
    }

    /**
     * The native executable of the running launcher (empty when run from a plain JVM, e.g. an IDE or a bare jar).
     */
    private static Optional<Path> currentExecutable() {
        return ProcessHandle.current().info().command()
                .map(Path::of)
                .filter(path -> !path.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("java"));
    }

    /**
     * Installs the update and restarts the launcher.
     *
     * @return false when this kind of installation can't be updated automatically (portable mode, unpackaged run,
     * no privilege helper on Linux...). The caller should then fall back to opening the download.
     */
    public static boolean installAndRestart(Path installer) throws IOException {
        if (Launcher.getInstance().isPortable()) return false;

        Optional<Path> executable = currentExecutable();
        if (executable.isEmpty()) return false;

        Path exe = executable.get();
        long pid = ProcessHandle.current().pid();
        Path dir = installer.getParent();

        ProcessBuilder builder;
        switch (OS) {
            case WINDOWS -> {
                Path script = dir.resolve("update.ps1");
                write(script, windowsScript(pid, installer, exe, dir.resolve("install.log")));
                builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-WindowStyle", "Hidden", "-File", script.toString());
            }
            case MAC -> {
                Path app = appBundle(exe);
                if (app == null) return false;
                Path script = dir.resolve("update.sh");
                write(script, macScript(pid, installer, app));
                builder = new ProcessBuilder("/bin/sh", script.toString());
            }
            case LINUX -> {
                String install = linuxInstallCommand(installer);
                if (install == null) return false;
                Path script = dir.resolve("update.sh");
                write(script, linuxScript(pid, install, exe));
                builder = new ProcessBuilder("/bin/sh", script.toString());
            }
            default -> {
                return false;
            }
        }

        builder.redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(dir.resolve("update-script.log").toFile()))
                .start();
        return true;
    }

    private static void write(Path script, String content) throws IOException {
        Files.writeString(script, content, StandardCharsets.UTF_8);
        script.toFile().setExecutable(true);
    }

    // ---------------------------------------------------------------- Windows

    private static String windowsScript(long pid, Path installer, Path exe, Path log) {
        // Per-user install (see winPerUserInstall in build.gradle.kts): no admin rights, no UAC prompt.
        String install = installer.getFileName().toString().endsWith(".msi")
                ? "Start-Process -FilePath 'msiexec.exe' -ArgumentList @('/i', '\"" + psq(installer) + "\"', '/passive', '/norestart', '/l*v', '\"" + psq(log) + "\"') -Wait"
                : "Start-Process -FilePath '" + psq(installer) + "' -Wait";
        return String.join("\r\n",
                "$ErrorActionPreference = 'Continue'",
                "try { Wait-Process -Id " + pid + " -Timeout 60 } catch { }",
                install,
                "Start-Process -FilePath '" + psq(exe) + "'",
                "");
    }

    private static String psq(Path path) {
        return path.toString().replace("'", "''");
    }

    // ---------------------------------------------------------------- macOS

    private static Path appBundle(Path executable) {
        for (Path p = executable; p != null; p = p.getParent()) {
            Path name = p.getFileName();
            if (name != null && name.toString().endsWith(".app")) return p;
        }
        return null;
    }

    private static String macScript(long pid, Path dmg, Path app) {
        return String.join("\n",
                "#!/bin/sh",
                "while kill -0 " + pid + " 2>/dev/null; do sleep 0.5; done",
                "MOUNT=$(mktemp -d /tmp/launchermc-update.XXXXXX)",
                "if hdiutil attach " + sq(dmg) + " -nobrowse -readonly -quiet -mountpoint \"$MOUNT\"; then",
                "  SRC=$(ls -d \"$MOUNT\"/*.app | head -n 1)",
                "  TARGET=" + sq(app),
                "  if ditto \"$SRC\" \"$TARGET.new\" && rm -rf \"$TARGET\" && mv \"$TARGET.new\" \"$TARGET\"; then",
                "    xattr -dr com.apple.quarantine \"$TARGET\" 2>/dev/null",
                "  else",
                "    # can't write to the install folder: let the user drag the app",
                "    rm -rf \"$TARGET.new\"",
                "    open " + sq(dmg),
                "    hdiutil detach \"$MOUNT\" -quiet",
                "    exit 1",
                "  fi",
                "  hdiutil detach \"$MOUNT\" -quiet",
                "  open \"$TARGET\"",
                "fi",
                "");
    }

    // ---------------------------------------------------------------- Linux

    private static String linuxInstallCommand(Path pkg) {
        // pkexec shows the desktop's graphical password prompt
        if (!hasCommand("pkexec")) return null;
        String file = sq(pkg);
        if (pkg.getFileName().toString().endsWith(".deb")) {
            return hasCommand("apt-get")
                    ? "pkexec apt-get install -y --allow-downgrades " + file
                    : "pkexec dpkg -i " + file;
        }
        if (hasCommand("dnf")) return "pkexec dnf install -y " + file;
        if (hasCommand("zypper")) return "pkexec zypper --non-interactive --no-gpg-checks install " + file;
        return "pkexec rpm -U --quiet " + file;
    }

    private static String linuxScript(long pid, String installCommand, Path exe) {
        return String.join("\n",
                "#!/bin/sh",
                "while kill -0 " + pid + " 2>/dev/null; do sleep 0.5; done",
                installCommand,
                "nohup " + sq(exe) + " >/dev/null 2>&1 &",
                "");
    }

    private static boolean hasCommand(String name) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(java.io.File.pathSeparator)) {
            if (!dir.isEmpty() && Files.isExecutable(Path.of(dir, name))) return true;
        }
        return false;
    }

    private static String sq(Path path) {
        return "'" + path.toString().replace("'", "'\\''") + "'";
    }
}
