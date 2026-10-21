---
name: build-and-verify
description: Use after changing Kotlin code, or when asked to build, test, lint, or check the project. Detects whether this is an Android app, Kotlin Multiplatform, or JVM backend/library build, picks the narrowest Gradle tasks that prove the change, runs them through gradle-run, and reports a compact pass/fail summary.
---

# Build and verify

Prove a change compiles, passes tests, and passes static checks with the
smallest set of Gradle tasks that covers it.

## 1. Detect the project shape

Read `settings.gradle.kts` and the build files of the modules you touched
(cache the answer for the session; re-read only if build files change).

| Signal in build files | Shape |
|---|---|
| `com.android.application` / `com.android.library`, no `kotlin("multiplatform")` | Android |
| `kotlin("multiplatform")` / `org.jetbrains.kotlin.multiplatform` | KMP (note its targets: `androidTarget`, `jvm`/desktop, `ios*`, `js`/`wasmJs`) |
| `kotlin("jvm")`, `application`, Ktor, Spring Boot | JVM |

Also note which checkers are applied: `spotless`, `ktlint` (jlleitschuh or
pinterest), `detekt`, Android `lint`, Metalava/`binary-compatibility-validator`.

## 2. Choose tasks (narrowest first)

Scope tasks to the touched modules (`:feature:login:...`), not the root,
unless a shared build file or version catalog changed.

| Shape | Compile | Unit tests | Static checks |
|---|---|---|---|
| Android | `:m:compileDebugKotlin` | `:m:testDebugUnitTest` | `:m:lintDebug` (only for resource/manifest/UI changes) |
| KMP | `:m:compileKotlinJvm` / `:m:compileDebugKotlinAndroid` | `:m:jvmTest` or `:m:allTests` (iOS tests cannot run on Windows; skip and say so) | per configured checker |
| JVM | `:m:compileKotlin` | `:m:test` (`--tests 'pkg.ClassTest'` when focused) | per configured checker |

Formatting/lint: run `spotlessCheck`, `ktlintCheck`, or `detekt` only if the
project applies them. If a format check fails, run the matching `*Apply` /
`*Format` task once, then re-check.

Quality gates (thresholds in `kotlin-architecture` §4): run `detekt` when
configured, and `koverVerify` for modules whose logic changed. If neither is
configured, say so in the report rather than skipping silently.

Public API changed in a library module? Also run `apiCheck` (BCV) or
`metalavaCheckCompatibility` if configured.

Instrumented tests (`connectedDebugAndroidTest`) need a device: hand off to the
`run-on-device` skill's device selection first, and only run them when the
change touches instrumented code or the user asks.

## 3. Execute

Run every Gradle command through the `gradle-run` skill.
It needs `python3`; if that or the JDK is missing, run `setup-environment`
instead of working around it.
Use `./gradlew` — never a global `gradle`. Gradle distributions and caches
are shared in `~/.gradle`; don't delete or relocate them.

Run from the directory that contains `gradlew` (it may be a subfolder of the
workspace; `settings.gradle.kts` marks the root). If there is no `gradlew` yet,
stop and say so; do not install Gradle.

## 4. Report

One short block:

```
Shape: Android | KMP(android,jvm,ios) | JVM
Ran:   :app:compileDebugKotlin ✅  :app:testDebugUnitTest ❌ (2 failed)  :app:lintDebug —skipped (no UI change)
Failures:
  - LoginViewModelTest.emitsErrorOnTimeout — expected Error, got Loading (LoginViewModel.kt:57)
Next: <fix you will make, or the question you need answered>
```

Do not paste full logs. Do not claim success for a task you did not run.
