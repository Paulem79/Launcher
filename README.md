# Launcher
A simple Launcher for Minecraft.<br><br>
Based on Bricklou's and Flow Updater's tutorial :<br>
https://github.com/Support-Launcher/javafx-launcher

**You'll need Java 21 to compile this project !**

## Updates
Each push on `launcher` publishes a release tagged `v<N>` (`N` = GitHub Actions run number). The CI passes `N` to
`build.gradle.kts` (`RELEASE_NUMBER` env var / `-PreleaseNumber`), which bakes it into the build
(`launcher-build.properties`) and into the package version (`1.3.<N>`).

At startup the launcher compares its own release number with the latest release. If newer, one click downloads the
installer for the OS, checks its sha256 and installs it, then restarts the launcher:
- Windows: MSI, no admin rights needed
- macOS: the `.app` is replaced from the DMG
- Linux: `.deb` / `.rpm` installed through `pkexec` (graphical password prompt)

Portable mode and local builds (release `0`) are never auto-updated.

## Star History
<a href="https://star-history.com/#Paulem79/Launcher&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Paulem79/Launcher&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=Paulem79/Launcher&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=Paulem79/Launcher&type=Date" />
 </picture>
</a>