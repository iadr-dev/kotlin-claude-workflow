---
name: kotlin-architecture
description: Use when creating modules, adding a feature that spans layers, deciding where code belongs, reviewing a change for architecture/maintainability/scalability, setting up code standards or quality gates (detekt, ktlint, Kover, dependency rules), or judging architecture metrics. Covers clean architecture, modularization, and code-quality thresholds for Android, Kotlin Multiplatform, and JVM backend projects. Complements the Kotlin/Compose skills (kotlin-*, compose-*), which own code-level Kotlin/Compose decisions.
---

# Kotlin architecture and code quality

Architecture rules decide **where** code lives and **what may depend on what**.
Code-level decisions (Flow design, `when`, Compose state) stay with the
`kotlin-*` and `compose-*` skills; load those for the code inside each layer.

## 1. Layers and the dependency rule

### Call flow and data flow

```
 PRESENTATION                         DOMAIN                         DATA
┌──────────────┐ events ┌───────────┐ calls ┌──────────┐ calls ┌────────────────────┐ calls ┌──────────────┐
│ UI           │──────▶│ ViewModel │──────▶│ UseCase  │──────▶│ Repository         │──────▶│ DataSource   │
│ (Composable) │       │ (state    │       │(optional)│       │ interface (domain) │       │ remote/local │
│              │◀──────│  holder)  │◀──────│          │◀──────│ impl (data)        │◀──────│ (Retrofit,   │
└──────────────┘ UiState└───────────┘domain └──────────┘domain └────────────────────┘ DTO / │ Ktor, Room,  │
                  (StateFlow)         models             models                  entity │ DataStore)   │
                                                                                        └──────────────┘
Compile-time dependencies: presentation → domain ← data   (data implements domain's interfaces)
```

Events go down (user action → ViewModel function → use case/repository).
State comes up (data source → repository maps to domain model → ViewModel
maps to `UiState` → UI renders). This is unidirectional data flow (UDF).

### What each piece owns

| Piece | Layer / module | Owns | Must not |
|---|---|---|---|
| **UI** (composables, screens) | presentation, `:feature:x` | rendering `UiState`, forwarding user events as lambdas, UI-only state (scroll, focus, animation, text field drafts) | call repositories/use cases, hold business logic, know DTOs |
| **UiState** | presentation, `:feature:x` | an immutable snapshot of everything the screen shows: `data class XUiState(...)` or a sealed `Loading / Content / Error`; already formatted for display | contain DTOs/entities, mutable collections, `Context`, framework types |
| **UI event / effect** | presentation | one-off outcomes (navigate, show snackbar) — prefer modeling them as state that the UI consumes (see `compose-state-and-effects`, `kotlin-concurrency-and-flow`) | be a hot `SharedFlow` without replay rules |
| **ViewModel** (state holder) | presentation, `:feature:x` | exposing `StateFlow<UiState>`, handling events, mapping domain models/errors → `UiState`, screen-level coroutine scope | do I/O directly, reference Views/`Context`/composables, expose `MutableStateFlow` |
| **UseCase** (optional) | domain, `:core:domain` or `:feature:x:domain` | one business operation: `class GetVisibleOrdersUseCase { operator fun invoke(...) }`; pure Kotlin, testable without Android | be a pass-through; hold state; depend on data implementations |
| **Domain model** | domain | entities and value classes with business meaning (`OrderId`, `Money`), domain errors (sealed) | carry serialization/DB annotations |
| **Repository interface** | domain | the contract the app needs (`fun observeOrders(): Flow<List<Order>>`) | leak where data comes from |
| **Repository implementation** | data, `:core:data` | single source of truth, caching/offline policy, merging remote + local, mapping DTO/entity → domain, exception → domain error, dispatcher selection | expose DTOs, be called from UI directly |
| **DataSource** | data | one source each: `OrdersRemoteDataSource` (Retrofit/Ktor), `OrdersLocalDataSource` (Room/SQLDelight/DataStore); DTOs and entities | contain business rules |

### When to add a UseCase

Add one when **any** is true:

1. **Combines** several repositories (e.g. orders + user settings → visible orders).
2. **Holds a business rule** worth testing on its own (validation, pricing, permission checks, sorting/filtering by business meaning).
3. **Reused** by two or more ViewModels (or by ViewModel + background worker/server route).
4. **Long or multi-step operation** that would bloat the ViewModel (checkout: validate → reserve → pay → confirm).
5. The project is a **KMP** project and the logic must be shared across platforms' presentation layers.

Skip it (ViewModel calls the repository directly) when the operation is a
single repository call with no logic — `observeOrders()`, `getUser(id)`.
Don't create a use case per repository method "for consistency"; if the
project already mandates that, follow the project's existing convention.

Use-case conventions: verb-first name (`GetX`, `ObserveX`, `PlaceOrder`), one
public `operator fun invoke`, `suspend` for one-shot work and `Flow` for streams,
inject dispatchers rather than hard-coding them, return domain types or a sealed
result.

### Variations by project type

- **Android app**: exactly the diagram. ViewModel = `androidx.lifecycle.ViewModel`.
- **KMP**: domain + data in `commonMain` (Ktor, SQLDelight/Room KMP, DataStore); ViewModel in `commonMain` with the KMP lifecycle-viewmodel artifact or a plain state holder; only UI and platform data sources live in platform source sets.
- **JVM backend**: no UiState/ViewModel. `Route/Controller → UseCase (service) → Repository → DataSource`. Request/response DTOs live in the delivery layer and are mapped at its edge; use cases are usually justified here because most endpoints carry business rules and transactions.
- **Small app/prototype**: domain may be a package inside one module instead of a separate module; keep the dependency direction anyway so it can be split later.

### Rules

- Domain is pure Kotlin. In KMP it lives in `commonMain`; on Android it is a `kotlin("jvm")` or KMP module, never an Android library.
- DTOs and DB entities never cross into domain or UI; map at the data boundary. Domain models are mapped to `UiState` in the ViewModel (or a small presentation mapper).
- Errors: data layer converts exceptions into domain errors (sealed type or `Result`); ViewModel maps them to user-facing `UiState`; presentation never catches `IOException`/`SQLException`.
- DI (Hilt/Koin/Metro/Spring) wires implementations at the app/composition root. Domain has no DI annotations other than `@Inject constructor` where the project already allows it.
- Tests per layer: UseCase and repository with fakes (`:core:testing`), ViewModel with fake repositories + `runTest`/Turbine, UI with `compose-ui-testing-patterns`.

## 2. Modularization

Default module graph (scale down for small projects; don't create empty modules):

```
:app                       (Android app / server main / KMP host apps)
:feature:<name>            UI + state holders for one user-facing feature
   └─ optionally :feature:<name>:api + :impl when other features must navigate to/consume it
:core:domain               shared entities, repository interfaces
:core:data                 repository impls, network, database
:core:ui / :core:designsystem   shared composables, theme
:core:testing              fakes, test rules, fixtures
build-logic/               convention plugins (included build)
```

Rules:

- `:feature:*` never depends on another `:feature:*` implementation — only on its `:api` module or via navigation contracts.
- `:core:*` never depends on `:feature:*`.
- Shared build config goes in `build-logic` convention plugins (`<proj>.android.library`, `<proj>.kmp.library`, `<proj>.jvm.library`, `<proj>.detekt`), not copy-pasted blocks. Versions only in `gradle/libs.versions.toml`.
- Prefer `implementation` over `api`; use `api` only when a type from the dependency appears in the module's public signatures.
- Mark everything `internal` by default; public only what other modules use. Library modules use `explicitApi()`.
- Split a module when: it exceeds ~10k LOC, two teams edit it, or a change in it rebuilds many unrelated modules. Merge modules that always change together.

## 3. Code standards

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html); Android projects also follow the Android Kotlin style guide.
- Formatting is enforced by tools, never by review: ktlint (or ktfmt) via Spotless, with `.editorconfig` committed.
- Static analysis: detekt with the project `config/detekt/detekt.yml` (template in `references/quality-gates.md`); Android lint for Android modules. New code adds no new baseline entries.
- Naming: packages by feature (`com.x.feature.login`), not by type (`com.x.viewmodels`). Classes are nouns, functions verbs, booleans `is/has/should`.
- No `!!` outside tests; no `lateinit` for values that can be constructor parameters; no `GlobalScope`; no mutable public state (`MutableStateFlow` exposed publicly).
- Immutability first: `val`, read-only collections, `data class` copies for state.
- Public APIs get KDoc when the behavior is not obvious from the name and types.

## 4. Quality metrics and thresholds

Use these as review findings and as detekt/Kover gates. Exceeding one is a
finding to justify or fix, not an automatic rewrite.

| Metric | Threshold | Tool |
|---|---|---|
| Cyclomatic complexity per function | ≤ 15 (warn > 10) | detekt `CyclomaticComplexMethod` |
| Cognitive complexity per function | ≤ 15 | detekt `CognitiveComplexMethod` |
| Function length | ≤ 60 lines | detekt `LongMethod` |
| Class length | ≤ 600 lines | detekt `LargeClass` |
| Parameters | ≤ 6 function / 7 constructor (composables exempt for modifiers/slots) | detekt `LongParameterList` |
| Nesting depth | ≤ 4 | detekt `NestedBlockDepth` |
| Functions per class | ≤ 20 public | detekt `TooManyFunctions` |
| File length | ≤ 500 lines | detekt `LargeClass`/review |
| Test coverage (line) | domain ≥ 80%, data ≥ 70%, overall ≥ 60%; changed lines covered | Kover `koverVerify` |
| Module dependency rule | zero violations of §1/§2 | review, `./gradlew :app:dependencies`, or a module-graph assertion plugin |
| Public API drift (libraries) | zero unreviewed changes | BCV `apiCheck` / Metalava |
| Build health | no new compiler warnings; `allWarningsAsErrors` in CI where feasible | Kotlin compiler |

## 5. Performance (architecture-level)

- **Android startup/runtime**: Baseline Profiles (`:baselineprofile` module), R8 full mode in release (`r8-analyzer` skill for keep rules), lazy DI for heavy graphs, no disk/network on main thread (StrictMode in debug).
- **Compose**: stable UI state models (immutable, `@Immutable`/`@Stable` only when true); defer to `compose-performance` for diagnosis.
- **Coroutines**: inject dispatchers (never hard-code `Dispatchers.IO` in classes); defer to `kotlin-concurrency-and-flow`.
- **Data**: paging for unbounded lists, caching policy defined in repositories, DB queries off main thread and indexed.
- **JVM server**: non-blocking I/O in Ktor handlers, connection pools sized explicitly, no N+1 queries.
- **Build performance**: configuration cache + build cache on, convention plugins, no `allprojects {}` / cross-project configuration, KSP over kapt.
- Measure before optimizing: Macrobenchmark / `android-profiler` skill on Android, JMH or load tests on JVM.

## 6. Scalability checklist (new feature or module)

1. Which layer and module owns each new type? (state the dependency direction)
2. Does any new dependency break §1/§2?
3. Is the public surface minimal (`internal` by default)?
4. Are fakes for new interfaces added in `:core:testing`?
5. Do metrics in §4 still hold for touched files?
6. Can the feature be removed by deleting its module plus one DI/navigation registration?

## 7. Bootstrapping quality gates

When the project lacks them, propose (don't silently add) the setup in
`references/quality-gates.md`: Spotless + ktlint, detekt config, Kover,
convention plugins, and the CI `check` aggregate. Ask before adding plugins.
