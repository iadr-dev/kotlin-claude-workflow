# Kotlin workflow for Claude Code

A ready-to-use [Claude Code](https://code.claude.com) setup for Kotlin projects:
Android (Jetpack Compose), Kotlin Multiplatform, and JVM backends or libraries.
Everything lives in this folder (`.claude/`, `CLAUDE.md`), so it travels with the
repository and has no runtime dependency on any other repository.

It gives Claude, in one place:

- a **feature workflow** (plan, implement, test, verify on a device, review, ship),
- **Kotlin and Compose skills** that load only when the code needs them,
- **architecture and quality rules** (clean architecture, modularization, metric thresholds),
- **Android tooling** through the official `android` CLI and Android skills,
- **review agents**, a **format hook**, and **Kotlin language-server** support,
- a **machine check** that finds what is missing or outdated on any computer.

## Contents

- [Quick start](#quick-start)
- [Install into your own project](#install-into-your-own-project)
- [The workflow](#the-workflow)
- [Commands you type](#commands-you-type)
- [What Claude does on its own](#what-claude-does-on-its-own)
- [Skills](#skills)
- [Agents](#agents)
- [Hook, plugins and permissions](#hook-plugins-and-permissions)
- [Architecture rules](#architecture-rules)
- [GitHub and pull requests](#github-and-pull-requests)
- [Project layout](#project-layout)
- [Sample app](#sample-app)
- [Status](#status)
- [Troubleshooting](#troubleshooting)

## Quick start

1. Install [Claude Code](https://code.claude.com) and open it in this folder.
2. Ask Claude to check the machine:

   > run setup-environment

   It runs `bash .claude/skills/setup-environment/scripts/check-env.sh .`, prints a
   table (`OK`, `UPDATE`, `OLD`, `MISSING`, `OPTIONAL`) and proposes install
   commands for your OS. Nothing is installed without your approval.
3. Accept the plugin prompts that Claude Code shows for `.claude/settings.json`.
   Install any plugin that is reported `MISSING`:
   `claude plugin install <name>@claude-plugins-official --scope project`.
4. Restart Claude Code after installing tools, so a new `PATH` is picked up.

### What the machine needs

| Tool | Why | Required |
|---|---|---|
| git | version control | yes |
| JDK 17 or newer (latest LTS recommended) | Gradle, Android Gradle Plugin | yes |
| `python3` | the `gradle-run` skill | yes (on Windows, `python3` must resolve; a one-line shim to `python` works) |
| `gh` | GitHub workflows (`gh auth login`) | yes |
| `android` CLI, Android SDK, `adb` | build, run and inspect Android apps | for Android work |
| `kotlin-lsp` on `PATH` | code intelligence for `.kt` files | optional |
| `ktlint` or `ktfmt` on `PATH` | auto-format hook | optional |

Gradle itself is never installed globally. Always use the project's `./gradlew`.
Its shared cache is `~/.gradle`.

## Install into your own project

Quick start above is for using this folder as it is. To add the workflow to a
project you already have, copy it into that project's root. Everything is plain
files, so the whole team gets it through git.

**Option A: GitHub template (new project).** On the repository page choose
*Use this template*, then delete `tasks-app/` and the "Sample app" section of
`CLAUDE.md`.

**Option B: copy into an existing project** (Git Bash, macOS or Linux):

```sh
git clone https://github.com/<owner>/kotlin-claude-workflow.git /tmp/kcw   # private repo: run `gh auth login` first
cd /path/to/your-project
cp -r /tmp/kcw/.claude .
cp /tmp/kcw/CLAUDE.md .
cat /tmp/kcw/.gitignore >> .gitignore            # keeps settings.local.json and caches out of git
cp -n /tmp/kcw/.gitattributes .                  # LF endings for shell scripts and gradlew
rm -rf /tmp/kcw
```

Then, in your project:

1. Edit `CLAUDE.md`: delete the "Sample app" section and adjust the build commands to your modules.
2. Do **not** copy `tasks-app/` or `.github/workflows/ci.yml`; the workflow file is written for the sample app, so write your own that runs your Gradle checks.
3. Restart Claude Code in the project and accept the plugin prompts from `.claude/settings.json`.
4. Ask Claude to run `setup-environment` and follow its proposals.
5. Try it: `/new-feature <a small change>`.

If a `.claude/` folder or `CLAUDE.md` already exists, merge instead of
overwriting: copy the folders under `.claude/skills` and `.claude/agents`, and
merge `settings.json` by hand.

**Plugin form (later).** Skills, agents and hooks can also be packaged as a Claude
plugin (`.claude-plugin/plugin.json` plus a marketplace entry, installed with
`/plugin install`) so several projects share one updated copy. A plugin cannot
carry `CLAUDE.md` or permissions, and scripts must use `${CLAUDE_PLUGIN_ROOT}`
paths, so this repository ships as copyable files for now.

## The workflow

```
 idea ──► plan ──► implement ──► verify ──► review ──► ship
          │         │              │          │         │
          │         │              │          │         └─ commit, or pull request
          │         │              │          └─ kotlin-reviewer (+ /review-pr agents)
          │         │              └─ build-and-verify, then run-on-device
          │         └─ the Kotlin/Compose skill that owns each decision, tests alongside
          └─ architecture checklist: which layer and module owns each new type
```

Claude pauses for you at two points only: **plan approval** and **ship**
(commit or PR). It never merges without explicit words from you.

## Commands you type

Two commands only run when you type them:

| Command | Example |
|---|---|
| `/new-feature <description or issue #>` | `/new-feature Add a "Clear completed" button that deletes all completed tasks` |
| `/review-pr [PR number] [aspects] [parallel]` | `/review-pr` · `/review-pr errors tests` · `/review-pr 12 parallel` |

`/review-pr` needs a diff to look at: uncommitted changes in a git repository, or a
GitHub pull request number. Aspects are `code`, `errors`, `tests`, `types`,
`comments`, or `all`.

Also available (shipped by the official `commit-commands` plugin): `/commit`,
`/commit-push-pr`.

## What Claude does on its own

Claude loads these when the situation matches. You can also name one directly.

| You say | Skill |
|---|---|
| "build / test / check it" | `build-and-verify` |
| "run it", "show me on the device", "take a screenshot" | `run-on-device` |
| "set up this machine", "is everything up to date?" | `setup-environment` |
| "where should this code live?", "new module", "review the architecture" | `kotlin-architecture` |
| "plan issue #12" | `to-plan` |

## Skills

All skills are plain folders under `.claude/skills/`. Each has a `SKILL.md`
whose description tells Claude when to use it.

### Project workflow

| Skill | Purpose |
|---|---|
| `new-feature` | End-to-end feature flow (user-typed command) |
| `review-pr` | Multi-agent review (user-typed command) |
| `build-and-verify` | Detect project shape; run the narrowest Gradle tasks that prove a change; compact pass/fail report |
| `run-on-device` | Pick a physical device or emulator, install, launch, inspect the UI with `android layout` and `android screen`, with safety rules for real phones |
| `setup-environment` | Machine and toolchain check, install guidance per OS |
| `kotlin-architecture` | Layers, modules, code standards, metric thresholds, quality-gate templates |
| `kotlin-skill-router` | Chooses which Kotlin or Compose skill owns a decision |

### Kotlin and Compose

| Skill | Use it for |
|---|---|
| `kotlin-concurrency-and-flow` | Coroutine scopes, cancellation, `StateFlow`/`SharedFlow`, one-shot events |
| `kotlin-control-flow` | `when`, sealed exhaustiveness, smart casts, nullable branches |
| `kotlin-api-design` | Function ownership, value and data classes, `expect`/`actual` |
| `compose-state-and-effects` | State ownership, hoisting, effects, Flow collection |
| `compose-performance` | Recomposition, stability, deferred reads |
| `compose-component-design` | Reusable component APIs, modifiers, slots |
| `compose-animations` | Choosing the right animation API |
| `compose-focus-navigation` | Keyboard, TV and D-pad focus |
| `compose-ui-testing-patterns` | UI, semantics and screenshot tests |
| `android-benchmark-comparison` | Comparing benchmark runs on physical devices |
| `release-kotlin-library` | Release readiness for a Kotlin library |

### Engineering practice

| Skill | Purpose |
|---|---|
| `gradle-run` | Runs every Gradle command through a compact-output wrapper; recovers status by workflow ID. On Windows it launches `gradlew.bat` |
| `tdd` | Test-first loop, agreed test seams, what makes a good test |
| `diagnosing-bugs` | Disciplined loop for hard bugs and regressions |
| `grilling`, `domain-modeling`, `prototype`, `research` | Stress-test a plan, build a glossary and decision records, throwaway prototypes, sourced research notes |
| `grounded-writing` | Evidence-based documentation and review text |

### GitHub

| Skill | Purpose |
|---|---|
| `to-plan` | Implementation plan from one issue or an in-chat task |
| `deliver-spec` | Deliver an approved spec through a reviewed pull request |
| `shepherd` | Poll a PR, triage review comments, repair CI, never merge on its own |
| `implement-with-subagents` | Delegated implementation of supplied tickets |
| `run-github-project` | Operate a GitHub Project board workflow |
| `triage`, `wayfinder`, `setup-issue-tracker` | Issue triage, large-work planning, tracker and label setup |

See [`.claude/pr-workflow.md`](.claude/pr-workflow.md) for the step-by-step PR flow.

### Android (official, installed with the `android` CLI)

Installed at user level by `android skills add <id>`, not stored here. Examples:
`navigation-3`, `edge-to-edge`, `adaptive`, `testing-setup`, `agp-9-upgrade`,
`r8-analyzer`, `android-profiler`, `jetpack-compose-performance`,
`anr-audit-and-fix`, `android-permissions-security`. Keep them current with
`android skills update --all`. Look up current API guidance with
`android docs search "<query>"`, then `android docs fetch kb://...`.

## Agents

Defined in `.claude/agents/`. Claude delegates to them; `/review-pr` runs several at once.

| Agent | Role |
|---|---|
| `kotlin-reviewer` | Read-only review of a diff: correctness, architecture rules, metric thresholds, Compose misuse |
| `silent-failure-hunter` | Swallowed exceptions, lost `CancellationException`, unjustified fallbacks |
| `pr-test-analyzer` | Behavior gaps and tests that cannot fail |
| `type-design-analyzer` | Encapsulation and invariants of Kotlin types |
| `comment-analyzer` | KDoc and comments that are wrong, stale or redundant |
| `android-verifier` | Drives a user flow on a device or emulator and reports what it saw, with screenshots |

## Hook, plugins and permissions

**Format hook** (`.claude/hooks/format-kotlin.sh`): after Claude edits a `.kt` or
`.kts` file, it runs `ktlint -F` (or `ktfmt`) on that file if one is on `PATH`.
It never blocks an edit; with no formatter installed it does nothing.

**Plugins** enabled in `.claude/settings.json` (all official):
`kotlin-lsp` (code intelligence), `feature-dev`, `commit-commands`, `context7`
(current library docs), `security-guidance`. Unrelated plugins are switched off
for this project in your personal, git-ignored `.claude/settings.local.json`.

**Kotlin language server:** the plugin needs the `kotlin-lsp` program on `PATH`
(download the standalone build for your OS from the
[Kotlin LSP releases](https://github.com/Kotlin/kotlin-lsp/releases)). It imports
the Gradle project when Claude Code starts, so start Claude Code **after** the
Gradle project exists.

**Permissions:** the shared `.claude/settings.json` pre-approves only read-only
commands with fixed prefixes (`android info/docs/layout/screen`, `adb devices`,
`adb logcat`, `git status`, and a few exact `git diff` / `git log` forms). Open-ended
`git diff` and `git log` are not pre-approved: Git accepts abbreviated options such
as `--out=<file>`, so a prefix rule could be used to write files. Installing,
pushing, `pm`, `su`, `settings`, `rm`, reboot and `git push` always ask. Rules that can run code or drive a device
(`./gradlew`, `android run`, `adb shell input`) are **not** shared: put them in
your own git-ignored `.claude/settings.local.json` if you want them
pre-approved. Claude Code shows a trust notice when a folder pre-approves any
permission, so keep the shared list short.

## Architecture rules

Defined in `.claude/skills/kotlin-architecture/` and checked by `kotlin-reviewer`.

```
 PRESENTATION                  DOMAIN                   DATA
 UI ─events─► ViewModel ─► UseCase (optional) ─► Repository ─► DataSource
 UI ◄─UiState─ ViewModel ◄─ domain models ◄──── (maps DTO → domain) ◄─ DTO / entity
 Compile-time: presentation → domain ← data
```

- Dependencies point inward; domain is pure Kotlin.
- Add a use case when it combines repositories, holds a business rule, is reused, or is multi-step. Otherwise the ViewModel calls the repository.
- `internal` by default; no feature-to-feature implementation dependencies; versions only in `gradle/libs.versions.toml`; shared build logic in convention plugins.
- Thresholds: cyclomatic complexity ≤ 15, function ≤ 60 lines, class ≤ 600 lines, ≤ 6 parameters, nesting ≤ 4, line coverage domain ≥ 80% / data ≥ 70% / overall ≥ 60%.
- Folder, package and file naming (package mirrors path, file named after its main type, role suffixes, split at ~15 files) is in `references/structure-and-naming.md`.
- Quality-gate templates (Spotless and ktlint, detekt, Kover, convention plugins) are in `references/quality-gates.md`.

## GitHub and pull requests

Prerequisites: a GitHub `origin`, `gh auth login`, and CI that runs the Gradle
checks. The everyday flow:

1. create an issue, 2. `/to-plan #12`, 3. `deliver spec #12 as a pull request`,
4. `/review-pr 12`, 5. `shepherd PR #12`, 6. merge only when you say so.

Details and optional triage and project-board workflows: [`.claude/pr-workflow.md`](.claude/pr-workflow.md).

## Project layout

```
README.md                 this file
CLAUDE.md                 project instructions Claude reads every session
.claude/
  settings.json           shared: plugins, permissions, hooks
  settings.local.json     personal (git-ignored): plugins turned off here
  pr-workflow.md          GitHub flow
  hooks/format-kotlin.sh  auto-format hook
  agents/                 review and verification agents
  skills/                 all skills listed above
tasks-app/                small sample Android app used to exercise the workflow
```

## Sample app

`tasks-app/` is a small clean-architecture Android app (`:app`, `:feature:tasks`,
`:core:domain`, `:core:data`, `:core:testing`) that exists only to exercise this
workflow. It is safe to delete; if you do, also remove the "Sample app" section
from `CLAUDE.md`. Its Gradle root is `tasks-app/`:

```sh
cd tasks-app
./gradlew :core:domain:test :core:data:test :feature:tasks:testDebugUnitTest :app:assembleDebug
```

## Status

Checked on Windows 11 with Git Bash, Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21:

| Area | State |
|---|---|
| `setup-environment` check, `gradle-run`, `build-and-verify`, `run-on-device` on an emulator | tested |
| `kotlin-architecture`, Kotlin and Compose skills, `kotlin-reviewer` and the four review agents, `android-verifier` | tested (the new section 8 naming rules and the detekt templates have not been run) |
| Format hook with ktlint, kotlin-lsp (symbols, cross-module definitions, hover) | tested |
| `to-plan`, `deliver-spec`, `shepherd`, with CI, two independent reviews and a squash merge (issue #2 to PR #4) | tested on a real GitHub repository |
| `/new-feature` and `/review-pr` | not yet tested: they only run when you type them, and Claude is blocked from invoking them itself |
| `run-github-project`, `triage`, `wayfinder`, `setup-issue-tracker` | not yet tested (user-invoked; need a GitHub Project board and labels) |
| Physical Android device | not yet tested |
| macOS and Linux | not yet tested (scripts are written to be portable) |

## Troubleshooting

| Symptom | Fix |
|---|---|
| `gradle-run` fails with `python3: command not found` | Make `python3` resolve (Windows: a `~/bin/python3` shim containing `exec python "$@"`) |
| kotlin-lsp returns no results | Restart Claude Code after the Gradle project exists; check `kotlin-lsp` is on `PATH` |
| Emulator screenshot is completely black but the app has no errors | Launch a system app and capture it; if that is black too, `android emulator stop <avd>` then `android emulator start --cold <avd>` |
| `android run --apks …` installs but does not open the app | Add `--activity <package>.MainActivity` |
| `android docs <query>` is not a valid command | Use `android docs search "<query>"` |
| Wrapper upgrade exits with `9009` on Windows | Re-run once; the first pass rewrites the running `gradlew.bat` |
| A tool shows `MISSING` or `UPDATE` | Run `setup-environment` and follow its proposals |
