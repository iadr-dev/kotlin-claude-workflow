---
name: setup-environment
description: Use on a new machine, when a teammate first opens this project, when a tool required by the workflow is missing (gradle-run, kotlin-lsp, adb, android CLI, gh), or when asked to check that tools and project toolchain (Gradle wrapper, Kotlin, AGP) are installed and up to date. Works on Windows (Git Bash), macOS, and Linux.
---

# Set up the environment

Make no assumptions about this machine. Check, report, then install only
with the user's approval.

## 1. Check

```sh
bash .claude/skills/setup-environment/scripts/check-env.sh .
```

(`--offline` skips the latest-version lookups.) The script is read-only and
prints one line per tool: `OK`, `UPDATE`, `OLD` (below minimum), `MISSING`
(required), or `OPTIONAL`.

Show the table to the user, then a short list of proposed actions:
MISSING/OLD first, then UPDATE, then OPTIONAL. **[ask]** before installing or upgrading
anything; machine-wide changes (PATH, package managers) need explicit approval.

## 2. Install / update per tool

Prefer the OS package manager. Use the commands for the detected OS.

| Tool | Windows | macOS | Linux |
|---|---|---|---|
| git | `winget install Git.Git` (provides Git Bash, required by Claude Code on Windows) | `brew install git` | distro package |
| JDK (latest LTS, Temurin) | `winget install EclipseAdoptium.Temurin.<N>.JDK` | `brew install --cask temurin@<N>` | `sdk install java <N>-tem` (SDKMAN) or distro package |
| python3 | `winget install Python.Python.3.<minor>` then make `python3` resolve (see note) | `brew install python` | distro package |
| gh | `winget install GitHub.cli` | `brew install gh` | see cli.github.com |
| android CLI | `winget install Google.AndroidCLI` | `curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh \| bash` (x86_64: `darwin_x86_64`) | `curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh \| bash` |
| Android SDK + adb | `android sdk install platform-tools` (then `android sdk update`) | same | same |
| kotlin-lsp | standalone zip from the latest github.com/Kotlin/kotlin-lsp release → unpack to a user dir → add to user PATH (see note) | `brew install JetBrains/utils/kotlin-lsp` | standalone tar.gz → `ln -s <dir>/kotlin-lsp.sh ~/.local/bin/kotlin-lsp` |
| ktlint | release files `ktlint` + `ktlint.bat` from the latest github.com/ktlint/ktlint release (verify the SHA-256 shown by the GitHub API `digest` field) into a user dir on PATH, `chmod +x ktlint` (winget/choco have no package; `scoop install ktlint` if scoop is present) | `brew install ktlint` | release binary from github.com/ktlint/ktlint |

After installing the android CLI, run `android init` once, then `android skills list`
and add any missing Android skills with `android skills add <id>`.

Updates: `android update` (CLI), `winget upgrade --id Google.AndroidCLI` (Windows launcher, reported by the check), `android skills update --all`, `android sdk update`, `gh` / `brew upgrade` / `winget upgrade <id>`.

Verify every download against the published SHA-256 when one exists.

### Windows notes

- **python3**: Windows installs only `python`. Create a Git Bash shim on PATH
  (e.g. `~/bin/python3` containing `exec python "$@"`, `chmod +x`), or disable the
  Microsoft Store `python3` alias in *App execution aliases* if it intercepts the command.
- **kotlin-lsp**: the plugin launches the command `kotlin-lsp`. The Windows zip
  ships `kotlin-lsp.cmd` (deprecated wrapper around `bin\intellij-server.exe`);
  add the unpacked folder to the *user* PATH with
  `powershell -NoProfile -Command "[Environment]::SetEnvironmentVariable('Path', [Environment]::GetEnvironmentVariable('Path','User') + ';<dir>', 'User')"`
  (avoid `setx`, it truncates long PATHs). Restart Claude Code afterwards.
- Gradle needs no global install — always `./gradlew`. Gradle's shared cache
  lives in `~/.gradle` (`%USERPROFILE%\.gradle`); never delete it as a fix.

## 3. Claude Code side

- Declared plugins are not always installed by the startup prompt. For each
  plugin the script marks MISSING, run `claude plugin install <id> --scope project`,
  then restart Claude Code so its skills load.
- `gh auth login` if `gh auth status` fails.
- Device: an emulator (`android emulator list`, or `android emulator create` if empty)
  and/or a physical device with USB debugging — see `run-on-device`.

## 4. Project toolchain updates

When the script reports `UPDATE` for gradle-wrapper / kotlin / agp, don't bump
silently. Propose it as a separate change, in this order:

1. Check compatibility first: `android docs search "AGP <version> release notes"`,
   then `android docs fetch` the result; its Compatibility table lists the
   minimum Gradle and JDK. Kotlin/Gradle compatibility: kotlinlang.org.
2. **Wrapper first, on the current AGP** (a newer AGP may not configure on the
   old Gradle). Pass the official checksum from
   `https://services.gradle.org/distributions/gradle-<v>-bin.zip.sha256`:
   `./gradlew wrapper --gradle-version <v> --gradle-distribution-sha256-sum <sha>`,
   run twice through `gradle-run`. On Windows the first pass can exit `9009`
   because the task rewrites the running `gradlew.bat`; confirm
   `gradle/wrapper/gradle-wrapper.properties` changed, and the second pass exits 0.
3. Then Kotlin and AGP in `gradle/libs.versions.toml` (AGP majors through the
   `agp-9-upgrade` skill), then `build-and-verify`.

## 5. Re-check

Run the script again and report the final table.
