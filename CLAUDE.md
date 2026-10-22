# Kotlin project

Kotlin workspace that may hold Android (Compose) apps, Kotlin Multiplatform
projects, and JVM backends/libraries. Detect the shape from the build files
before acting (see `build-and-verify`).

## Environment

Machine-independent. Required tools: git, latest-LTS JDK (≥ 17), `python3`
(for `gradle-run`), `gh`, the `android` CLI with Android SDK platform-tools
(`adb`); optional: `kotlin-lsp` on PATH, ktlint/ktfmt. Gradle is never
installed globally — always `./gradlew`; its shared cache is `~/.gradle`.

On a new machine, or whenever a tool seems missing or outdated, use the
`setup-environment` skill (`bash .claude/skills/setup-environment/scripts/check-env.sh .`).
Read SDK paths, devices, and AVD names from the machine (`android info`,
`adb devices -l`, `android emulator list`) — never hard-code them.
iOS targets need macOS + Xcode; on Windows/Linux verify shared code with JVM tests.

## Commands

Run all Gradle through the `gradle-run` skill. Typical tasks (scope to touched modules):

- Build: `./gradlew :<module>:assembleDebug` (Android) / `:<module>:build` (JVM)
- Unit tests: `:<module>:testDebugUnitTest` (Android), `:<module>:jvmTest` / `allTests` (KMP), `:<module>:test` (JVM)
- Checks: `spotlessCheck`, `detekt`, `lintDebug`, `koverVerify`, `apiCheck` — whichever the project applies
- Run on device: `android run --device <serial>`

## Workflow

- New feature end-to-end: `/new-feature <description or issue #>`
- After any code change: `build-and-verify`; after UI changes: `run-on-device` (or the `android-verifier` agent)
- Before commit/PR: `kotlin-reviewer` agent on the diff
- GitHub: `to-plan` for issues, `deliver-spec` to ship a spec as a PR, `shepherd` to follow CI/review (steps and prompts: `.claude/pr-workflow.md`)

## Code rules

- Architecture, modules, code standards, and metric thresholds: `kotlin-architecture` skill. Key rules: dependencies point inward (presentation → domain ← data), domain is pure Kotlin, `internal` by default, no feature→feature implementation dependencies, versions only in `gradle/libs.versions.toml`, shared build config in `build-logic` convention plugins.
- Code-level Kotlin/Compose decisions: route through `kotlin-skill-router`.
- Android platform APIs: matching Android skill, and `android docs search "<query>"` (then `android docs fetch kb://...`) for current guidance. Library APIs: context7.
- Every behavior change ships with tests. Don't report work as verified unless the task actually ran.

## Setup for a new machine / teammate

Run the `setup-environment` skill. It checks every tool against its latest
version, proposes installs per OS, and covers plugins (declared in
`.claude/settings.json`), Android skills, `gh auth`, and devices.
