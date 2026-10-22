# Quality gates setup

Templates for bootstrapping. They were built and run on a real multi-module
project (Gradle 9.8.2, Kotlin 2.4.21, October 2026): every gate below runs
under `./gradlew check`, and each one failed `check` when deliberately violated.
Look up the latest plugin versions with context7 or the plugin portal before
adding; the versions listed are the ones that were tested.

**JDK note.** detekt 1.x (`io.gitlab.arturbosch.detekt` 1.23.8) and the
binary-compatibility-validator do not run on JDK 25 yet (detekt fails with a
bare JDK version error, the API dump with "Unsupported class file major
version"). The templates below use detekt 2.x (`dev.detekt`, alpha) and pin the
bytecode target to 17; CI runs Temurin 17. If you stay on detekt 1.x, run
Gradle on JDK 17 or 21.

## gradle/libs.versions.toml (excerpt)

```toml
[versions]
detekt = "2.0.0-alpha.6"   # dev.detekt 2.x; the key names in detekt.yml below are 2.x
spotless = "8.10.4"
ktlint = "1.8.0"
kover = "0.9.11"
bcv = "0.18.2"             # maintenance mode; Kotlin's built-in ABI validation is the successor

[libraries]
# plugins the convention plugin applies, as dependencies of build-logic
detekt-gradlePlugin = { module = "dev.detekt:detekt-gradle-plugin", version.ref = "detekt" }
spotless-gradlePlugin = { module = "com.diffplug.spotless:spotless-plugin-gradle", version.ref = "spotless" }
kover-gradlePlugin = { module = "org.jetbrains.kotlinx:kover-gradle-plugin", version.ref = "kover" }

[plugins]
detekt = { id = "dev.detekt", version.ref = "detekt" }
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
bcv = { id = "org.jetbrains.kotlinx.binary-compatibility-validator", version.ref = "bcv" }
```

## build-logic convention plugin (quality)

`build-logic/settings.gradle.kts` must read the same catalog:

```kotlin
dependencyResolutionManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}
rootProject.name = "build-logic"
include(":convention")
```

`build-logic/convention/build.gradle.kts` (use `implementation`, not
`compileOnly`: with `compileOnly` the plugin fails to load with
`NoClassDefFoundError`):

```kotlin
plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "app.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
```

```kotlin
// build-logic/convention/src/main/kotlin/QualityConventionPlugin.kt
import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("dev.detekt")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    ktlint(libs.findVersion("ktlint").get().requiredVersion)
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint()
                }
            }
            extensions.configure<DetektExtension> {
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                buildUponDefaultConfig.set(true) // 2.x properties use .set(), not "="
                baseline.set(file("detekt-baseline.xml"))
            }
            // Plain `detekt` skips the rules that need type resolution (InjectDispatcher,
            // UnsafeCallOnNullableType, ...). `detektMain` and `detektTest` run them but
            // are not attached to `check` by default, so attach every detekt task.
            tasks.named("check") { dependsOn(tasks.withType<Detekt>()) }
        }
    }
}
```

Apply it in each module with `plugins { id("app.quality") }`.

## config/detekt/detekt.yml (thresholds from SKILL.md §4, detekt 2.x keys)

detekt rejects unknown keys. In 2.x the 1.x `threshold` keys are renamed
`allowed*`, and the value is the **maximum allowed** (60 lines passes, 61 fails).

```yaml
complexity:
  CyclomaticComplexMethod:
    allowedComplexity: 15
    ignoreSimpleWhenEntries: true
  CognitiveComplexMethod:
    active: true
    allowedComplexity: 15
  LongMethod:
    allowedLines: 60
    ignoreAnnotated: ['Composable']   # composables measured in review instead
  LargeClass:
    allowedLines: 600
  LongParameterList:
    allowedFunctionParameters: 6
    allowedConstructorParameters: 7
    ignoreDefaultParameters: true
    ignoreAnnotated: ['Composable']
  NestedBlockDepth:
    allowedDepth: 4
  TooManyFunctions:
    allowedFunctionsPerClass: 20
    allowedFunctionsPerFile: 20
    ignorePrivate: true

style:
  ForbiddenComment:
    active: false
  MagicNumber:
    ignorePropertyDeclaration: true
    ignoreAnnotated: ['Composable', 'Preview']
  UnusedPrivateProperty:
    active: true
  UnusedPrivateFunction:
    active: true

potential-bugs:
  UnsafeCallOnNullableType:
    active: true
    excludes: ['**/test/**', '**/androidTest/**', '**/*Test.kt']

coroutines:
  GlobalCoroutineUsage:
    active: true
  InjectDispatcher:
    active: true
  RedundantSuspendModifier:
    active: true

naming:
  FunctionNaming:
    ignoreAnnotated: ['Composable']
  MatchingDeclarationName:
    active: true
  InvalidPackageDeclaration:
    active: true
    rootPackage: 'com.example.app'   # app-wide root: one value for every module
    requireRootInDeclaration: true
  ForbiddenClassName:
    active: true
    forbiddenName: ['*Manager', '*Helper', '*Util', '*Utils']   # glob patterns
```

For detekt 1.x, the keys are the old ones (`threshold`, `functionThreshold`,
`constructorThreshold`, `thresholdInClasses`, `thresholdInFiles`) and
`UnusedPrivateMember` instead of the two `UnusedPrivate*` rules.
`InjectDispatcher` and `UnsafeCallOnNullableType` only fire in the
type-resolution tasks, which the convention plugin above attaches to `check`.
For Compose projects add the `io.nlopez.compose.rules:detekt` rule set (not
covered by these tests).

## Kover thresholds

```kotlin
kover {
    reports {
        filters { excludes { annotatedBy("*Generated*", "androidx.compose.ui.tooling.preview.Preview") } }
        verify { rule { minBound(60) } }   // per-module overrides: domain 80, data 70
    }
}
```

`koverVerify` checks the overall line coverage of its own module, so it enforces
the module thresholds in SKILL.md §4 but not "changed lines covered".

## Public API check (libraries)

Apply `alias(libs.plugins.bcv)` in library modules and commit the generated
`api/` dump (`apiDump`); `apiCheck` is attached to `check`. Do not request
`apiDump` and `apiCheck` in the same invocation. The plugin cannot read JDK 25
bytecode, so pin the target in the module:

```kotlin
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
```

Kotlin's built-in ABI validation (`checkKotlinAbi`, attached to `check`) is the
successor and is still experimental in 2.4:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

kotlin { @OptIn(ExperimentalAbiValidation::class) abiValidation { } }
```

## Module dependency rule check (lightweight)

A root task that fails when a `:feature:*` module depends on another
`:feature:*` implementation, or a `:core:*` module depends on a `:feature:*`
module. `:feature:x:api` modules are allowed as targets. The root project needs
the `base` plugin so that it has a `check` task.

```kotlin
// root build.gradle.kts
import org.gradle.api.artifacts.ProjectDependency

plugins { base }

val checkModuleDependencies = tasks.register("checkModuleDependencies") {
    group = "verification"
    description = "No feature-to-feature implementation dependency; no core-to-feature dependency."
    val violations = provider {
        subprojects.flatMap { module ->
            listOf("api", "implementation", "compileOnly", "runtimeOnly")
                .mapNotNull { module.configurations.findByName(it) }
                .flatMap { configuration ->
                    configuration.dependencies.withType(ProjectDependency::class.java).mapNotNull { dep ->
                        val from = module.path
                        val to = dep.path
                        when {
                            from.startsWith(":feature:") && to.startsWith(":feature:") &&
                                !to.endsWith(":api") && from != to ->
                                "$from -> $to (${configuration.name}): feature depends on a feature implementation"
                            from.startsWith(":core:") && to.startsWith(":feature:") ->
                                "$from -> $to (${configuration.name}): core depends on a feature"
                            else -> null
                        }
                    }
                }
        }
    }
    doLast {
        val found = violations.get()
        check(found.isEmpty()) { "Module dependency rule violated:\n" + found.joinToString("\n") }
    }
}

tasks.named("check") { dependsOn(checkModuleDependencies) }
```

Plugins such as `com.autonomousapps.dependency-analysis` also report
unused/misdeclared (`api` vs `implementation`) dependencies.

## CI template (GitHub Actions)

Save the block as `.github/workflows/ci.yml`. It derives from a workflow that
ran green on `ubuntu-latest`. The template itself was run on GitHub Actions
against three small projects (Gradle 9.8.2, Temurin 17): a Kotlin JVM project
(green with a passing test, red with a broken one), an Android app with two
library modules (`./gradlew check assembleDebug`, about 2 minutes), and a Kotlin
Multiplatform library (green on `ubuntu-latest` in about 1 minute and on
`macos-latest` in about 3.5 minutes). Third-party actions are pinned by tag; see
the note after the block.

What `./gradlew check` already runs, as of Kover 0.9, Android Gradle plugin 8/9
and binary-compatibility-validator 0.18, plus the project's unit tests:

- Spotless: attached to `check` by its plugin.
- detekt: the plain `detekt` task is attached, but the rules that need type
  resolution (`InjectDispatcher`, `UnsafeCallOnNullableType`) only run in
  `detektMain` and `detektTest`, which are not attached by default. The
  convention plugin above attaches them; without it, `check` stays green on a
  hard-coded `Dispatchers.IO` or a `!!` in main code.
- Kover `koverVerify`: attached for the total variant; it only gates something
  when `verify { rule { ... } }` is defined. It is a per-project task, so each
  module that applies Kover runs its own. A variant task such as
  `koverVerifyDebug` is not attached; append it if the gate is variant-specific.
- Android lint: `check` runs `lint` for the default variant only (`debug` when
  there are no flavors). An error that exists only in `src/release` leaves `check`
  green; append `lintRelease` (or another `lint<Variant>`) to cover other variants.
- API check (`apiCheck`, binary-compatibility-validator): attached to `check`.
  The Kotlin Gradle plugin's ABI validation (`checkKotlinAbi`) attaches the same way.

So `./gradlew check` is the single default. The only optional appends are
`assembleDebug` (Android dexing and packaging, for example a class defined in two
modules; `check` already catches manifest and resource link errors),
`lint<Variant>` and `koverVerify<Variant>`.

```yaml
name: CI

on:
  push:
    branches: [main] # rename if your default branch differs
  pull_request:

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    # KMP: iOS tests (iosSimulatorArm64Test) only run on a macOS runner. On
    # ubuntu-latest `check` runs jvmTest and the Android host test, and the iOS test
    # tasks are skipped with a "Native task ... is disabled" warning; set
    # kotlin.native.ignoreDisabledTargets=true in gradle.properties to silence it.
    # macos-latest is Apple Silicon, so iosX64Test is skipped there too; keep
    # iosSimulatorArm64(). For iOS coverage run a second job with
    # `runs-on: macos-latest` (about 3.5 minutes against about 1 on Linux).
    runs-on: ubuntu-latest
    # If the Gradle root is a subfolder, uncomment:
    # defaults:
    #   run:
    #     working-directory: my-app
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17 # raise to your project's toolchain version
      - uses: gradle/actions/setup-gradle@v4
      - name: Check
        # Android: append assembleDebug to also catch dexing and packaging errors
        # (for example duplicate classes); check already catches resource and
        # manifest link errors. The Android SDK is already on ubuntu-latest, and
        # a missing platform is downloaded automatically.
        run: ./gradlew check
```

Pinning: the block uses tags (`@v4`), which keeps it readable. SonarCloud
(`githubactions:S7637`) and most supply-chain guidance want third-party actions,
here `gradle/actions/setup-gradle`, pinned to a full commit SHA instead
(`uses: gradle/actions/setup-gradle@<sha> # v4`). The first-party `actions/*`
were not flagged. Use the SHA form if your repository has such a gate.
