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

## CI aggregate

`./gradlew spotlessCheck detekt lint koverVerify test apiCheck` (drop tasks the
project doesn't apply). Run with configuration cache enabled.
