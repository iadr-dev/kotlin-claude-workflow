# Folders, packages and file naming

Goal: a reader (or Claude) can predict where a file lives from its name, and
find every file of a feature by its path. This matters most in large
projects, where hundreds of files make a flat or inconsistent layout slow to
search, easy to misplace into, and a constant source of merge conflicts.

Apply the rules in proportion: **do not create empty folders or sub-packages
ahead of need.** A small module may stay flat until it crosses the split
triggers below.

## 1. Packages

- The package mirrors the directory path, and every module has one root:
  `<org>.<app>.<module path>` (`:app` uses `<org>.<app>` directly).

  | Module | Root package |
  |---|---|
  | `:app` | `com.example.tasks` |
  | `:feature:tasks` | `com.example.tasks.feature.tasks` |
  | `:core:domain` | `com.example.tasks.core.domain` |
  | `:core:data` | `com.example.tasks.core.data` |
  | `:core:testing` | `com.example.tasks.core.testing` |

  Source directories may be `src/main/kotlin` or `src/main/java` (Android's
  default for an app module): the package must match the path below the source
  root, not the name of the source root.

- Group by feature or subdomain first, by role second. `task/` containing a
  repository, a use case and a mapper beats `repositories/` containing every
  repository of the app.
- Never name a package after a layer-wide technical bucket (`utils`, `helpers`,
  `common`, `misc`, `managers`, `viewmodels`, `adapters`).
- Lowercase, no underscores; a single word per segment where possible.

## 2. Layout inside a module

Grow into these shapes (the sample app stays flat) when the split triggers (section 4) fire.

**Feature module (presentation)**

```
feature/tasks/src/main/kotlin/com/example/tasks/feature/tasks/
  TasksScreen.kt        stateful entry composable + stateless TasksContent
  TasksViewModel.kt
  TasksUiState.kt       UiState, UI events/actions, item UI models
  components/           composables used only inside this feature
  navigation/           route keys and the graph entry, once there is more than one destination
  detail/               a sub-screen: its own Screen / ViewModel / UiState trio
```

**Domain module**: by subdomain, not by kind of class.

```
core/domain/src/main/kotlin/com/example/tasks/core/domain/
  task/       Task.kt, TaskRepository.kt, AddTaskUseCase.kt, GetVisibleTasksUseCase.kt
  settings/   SettingsRepository.kt
  error/      domain errors shared by several subdomains
```

**Data module**: the same subdomains, then by source.

```
core/data/src/main/kotlin/com/example/tasks/core/data/
  task/       DefaultTaskRepository.kt, TaskMapping.kt
  local/      Dao, Entity, LocalDataSource   (Room, SQLDelight, DataStore)
  remote/     Api, Dto, RemoteDataSource     (Retrofit, Ktor)
  DataModule.kt
```

**Testing module**: one fake per file, named after what it fakes
(`FakeTaskRepository.kt`).

**Tests mirror the source tree**: `src/test/kotlin/<same package>/<Subject>Test.kt`,
and the same for `androidTest`. A test file tests one subject.

**Kotlin Multiplatform**: `commonMain`, `androidMain`, `jvmMain`, `iosMain`
under `src/<sourceSet>/kotlin/<package>`. Platform files follow the Kotlin
convention `Name.android.kt` / `Name.jvm.kt` next to `Name.kt` for the
`expect` declaration.

**JVM backend**: feature first, then role: `orders/OrderRoutes.kt`,
`orders/OrderService.kt`, `orders/OrderRepository.kt`; shared plumbing in
`platform/` (HTTP, serialization, config), not in a catch-all `util/`.

**Android resources**: prefix with the feature: strings `tasks_title`,
`tasks_add`; drawables `ic_tasks_<name>`; keep a feature's resources in its own
module.

## 3. File naming

- A file is named after its primary top-level type: `TaskRepository.kt`.
- **Allowed exception:** one file may hold small types that always change
  together: a sealed hierarchy with its members, a value class with its entity
  (`Task.kt` = `TaskId` + `Task`), a use case or interface with its tiny result types
  (`AddTaskUseCase.kt` = the use case + `AddTaskResult` + `TitleError`). Name the
  file after the main type or the group concept (`TasksUiState.kt`).
- **Not an exception:** two independent aggregates or services in one file
  (two repositories, two fakes, a repository plus its data source). Split them.
- Extension-function files are named after what they do, not after the
  receiver or a generic bucket: `TaskMapping.kt`, `DurationFormatting.kt`. Never `Utils.kt`,
  `Helpers.kt`, `Common.kt`, `Misc.kt`, `Extensions.kt`.
- Suffixes carry the role, and each project picks one spelling and keeps it:

  | Role | Name |
  |---|---|
  | Entry composable / stateless composable | `TasksScreen` / `TasksContent` |
  | State holder | `TasksViewModel` |
  | UI state (with its item models and UI errors), events | `TasksUiState`, `TasksEvent` |
  | Use case (verb first) | `AddTaskUseCase`, `GetVisibleTasksUseCase` |
  | Repository interface (domain) | `TaskRepository` |
  | Repository implementation | `DefaultTaskRepository` (this repo) or `TaskRepositoryImpl` |
  | Data source, DAO, stored record, DTO | `TaskLocalDataSource`, `TaskDao`, `TaskEntity` or `TaskRecord` (pick one), `TaskDto` |
  | Mapping | `toDomain()` / `toDto()` in `TaskMapping.kt` |
  | Fake / test | `FakeTaskRepository` / `TaskRepositoryTest` |
  | Composition root / DI | `AppContainer`, `DataModule` |

- Avoid vague class names (`Manager`, `Helper`, `Util`, `Handler`, `Processor`)
  unless the word is the domain's own term.
- Acronyms follow PascalCase: `HttpClient`, `UiState`, `UserId`, not `HTTPClient`.
- Test names describe behavior: `clearCompletedWaitsForInFlightWrite`, never
  `test1` or `works`.

## 4. Split triggers

Split or reorganize when any of these holds:

- more than ~15 files in one package or folder: introduce sub-packages by
  subdomain, or by role inside a feature;
- a file passes 500 lines, or holds more than ~5 unrelated types;
- a composable file holds more than ~3 composables that are not private parts
  of the main one: move the reusable ones to `components/`;
- a feature module needs a second destination: add `navigation/` and give the
  sub-screen its own folder;
- `:core:ui` or `:core:designsystem` grows past ~15 components: group by
  component (`button/`, `dialog/`, `list/`), not alphabetically.

Moving a file never changes behavior: do the move in its own commit, with no
edits, so history and review stay readable.

## 5. Enforcement

Prefer tools to review comments. Detekt rules (templates in `quality-gates.md`).
`MatchingDeclarationName` and `InvalidPackageDeclaration` are on by default;
`ForbiddenClassName` is not:

- `MatchingDeclarationName`: a file with a single non-private class is named
  after it. It does not judge files with several types, so the "main type" rule
  for those is a review item. ktlint's `standard:filename` rule covers similar
  ground: enable one of them, not both.
- `InvalidPackageDeclaration`: the package matches the directory. `rootPackage`
  is a single value in the shared `detekt.yml`, so set it to the app-wide root
  (for example `com.example.tasks`); it cannot differ per module there.
- `ForbiddenClassName`: `forbiddenName` takes glob patterns, so use
  `['*Manager', '*Helper', '*Util', '*Utils']`. A bare `Manager` only matches a
  class named exactly `Manager`.

`kotlin-reviewer` flags the rest: file names such as `Utils.kt` (no detekt rule
checks file names against a deny list), files in the wrong layer or module, two
independent aggregates in one file, a package or folder past the split trigger,
vague names, and tests that do not mirror their subject.

The sample app in this repository is deliberately flat (far below the split
triggers) and is not gated by detekt; adopt the templates in your own project.
