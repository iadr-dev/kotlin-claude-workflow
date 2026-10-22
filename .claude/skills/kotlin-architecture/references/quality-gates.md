# Quality gates setup

Templates for bootstrapping. Check the latest plugin versions with context7 or
the plugin portal before adding; versions below are placeholders in the catalog.

## gradle/libs.versions.toml (excerpt)

```toml
[versions]
detekt = "<latest>"
spotless = "<latest>"
ktlint = "<latest>"
kover = "<latest>"

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }   # or dev.detekt for detekt 2.x
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
```

## build-logic convention plugin (quality)

```kotlin
// build-logic/convention/src/main/kotlin/QualityConventionPlugin.kt
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.diffplug.spotless")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                ktlint(libs.findVersion("ktlint").get().requiredVersion)
            }
            kotlinGradle { target("*.gradle.kts"); ktlint() }
        }
        extensions.configure<DetektExtension> {
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            buildUponDefaultConfig = true
            baseline = file("detekt-baseline.xml")
        }
    }
}
```

## config/detekt/detekt.yml (thresholds from SKILL.md §4)

```yaml
complexity:
  CyclomaticComplexMethod:
    threshold: 15
    ignoreSimpleWhenEntries: true
  CognitiveComplexMethod:
    active: true
    threshold: 15
  LongMethod:
    threshold: 60
    ignoreAnnotated: ['Composable']   # composables measured in review instead
  LargeClass:
    threshold: 600
  LongParameterList:
    functionThreshold: 6
    constructorThreshold: 7
    ignoreDefaultParameters: true
    ignoreAnnotated: ['Composable']
  NestedBlockDepth:
    threshold: 4
  TooManyFunctions:
    thresholdInClasses: 20
    thresholdInFiles: 20
    ignorePrivate: true

style:
  ForbiddenComment:
    active: false
  MagicNumber:
    ignorePropertyDeclaration: true
    ignoreAnnotated: ['Composable', 'Preview']
  UnusedPrivateMember:
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

For Compose projects add the `io.nlopez.compose.rules:detekt` rule set.

## Kover thresholds

```kotlin
kover {
    reports {
        filters { excludes { annotatedBy("*Generated*", "androidx.compose.ui.tooling.preview.Preview") } }
        verify { rule { minBound(60) } }   // per-module overrides: domain 80, data 70
    }
}
```

## Module dependency rule check (lightweight)

Without a plugin, assert in a root task or test that no `:feature:*` project
depends on another `:feature:*` implementation and no `:core:*` depends on
`:feature:*`, by walking `project.configurations["implementation"].dependencies`
of type `ProjectDependency`. Plugins such as `com.autonomousapps.dependency-analysis`
also report unused/misdeclared (`api` vs `implementation`) dependencies.

## CI template (GitHub Actions)

Save the block as `.github/workflows/ci.yml`. It derives from a workflow that
ran green on `ubuntu-latest`, but this template itself has not been run end to
end.

What `./gradlew check` already runs, as of Kover 0.9, Android Gradle plugin 8/9
and binary-compatibility-validator 0.18, plus the project's unit tests:

- Spotless and detekt: attached to `check` by their plugins.
- Kover `koverVerify`: attached for the total variant; it only gates something
  when `verify { rule { ... } }` is defined. It is a per-project task, so each
  module that applies Kover runs its own. A variant task such as
  `koverVerifyDebug` is not attached; append it if the gate is variant-specific.
- Android lint: `check` runs `lint` for the default variant only. Append
  `lintRelease` (or another `lint<Variant>`) to cover other variants.
- API check (`apiCheck`, binary-compatibility-validator): attached to `check`.
  The Kotlin Gradle plugin's ABI validation (`checkKotlinAbi`) attaches the same way.

So `./gradlew check` is the single default. The only optional appends are
`assembleDebug` (Android packaging and resources), `lint<Variant>` and
`koverVerify<Variant>`.

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
    # KMP: iOS tests need a macOS runner; Linux covers the JVM and Android targets.
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
        # Android: append assembleDebug to also catch packaging and resource
        # errors (the Android SDK is already on ubuntu-latest).
        run: ./gradlew check
```
