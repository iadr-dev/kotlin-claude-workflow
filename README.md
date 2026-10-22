# Kotlin workflow for Claude Code

A ready-to-use [Claude Code](https://code.claude.com) setup for Kotlin projects:
Android (Jetpack Compose), Kotlin Multiplatform, and JVM backends or libraries.
The workflow itself lives in `.claude/` and `CLAUDE.md`, so it travels with the
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
- [Status](#status)
- [Troubleshooting](#troubleshooting)
- [License](#license)

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

**Option A: clone it (new project).** This is a normal repository:

```sh
git clone https://github.com/iadr-dev/kotlin-claude-workflow.git my-project
cd my-project
```

Then adjust the build commands in `CLAUDE.md` to your modules, and reset the files
that are specific to this repository (see [Reset the repository-specific files](#reset-the-repository-specific-files)).
To start with a fresh history, `rm -rf .git && git init`.

**Option B: copy into an existing project** (Git Bash, macOS or Linux):

```sh
git clone https://github.com/iadr-dev/kotlin-claude-workflow.git /tmp/kcw
cd /path/to/your-project
cp -r /tmp/kcw/.claude .
cp /tmp/kcw/CLAUDE.md .
cat /tmp/kcw/.gitignore >> .gitignore            # keeps settings.local.json and caches out of git
cp -n /tmp/kcw/.gitattributes .                  # LF endings for shell scripts and gradlew
rm -rf /tmp/kcw
```

Then, in your project:

1. Edit `CLAUDE.md`: adjust the build commands to your modules.
2. Add CI that runs your Gradle checks on every pull request (see the [CI template (GitHub Actions)](.claude/skills/kotlin-architecture/references/quality-gates.md#ci-template-github-actions), which ran green on JVM, Android and KMP test projects); the PR skills use CI as their confirmation. This repository ships no workflow file because it contains no app to build.
3. Restart Claude Code in the project and accept the plugin prompts from `.claude/settings.json`.
4. Ask Claude to run `setup-environment` and follow its proposals.
5. Try it: `/new-feature <a small change>`.

If a `.claude/` folder or `CLAUDE.md` already exists, merge instead of
overwriting: copy the folders under `.claude/skills` and `.claude/agents`, and
merge `settings.json` by hand.

### Reset the repository-specific files

This repository dogfoods its own GitHub setup, so a few files describe **this**
repository and its Project board, not yours:

- the `## Agent skills` block at the end of `CLAUDE.md` (tracker, labels, domain docs, Project);
- `docs/agents/` (tracker, labels, domain docs, and `run-github-project.md`, which names Project #2 of `iadr-dev`);
- `GLOSSARY.md` (the vocabulary of the environment check).

After a clone, delete `docs/agents/`, `GLOSSARY.md` and that block. After a copy
(Option B), do not copy `docs/agents/` and delete the block from the copied
`CLAUDE.md`. If you want the GitHub workflows, run `/setup-issue-tracker` (and later
`/run-github-project Set up the binding for <project URL>`) to write your own.

**Plugin form (later).** Skills, agents and hooks can also be packaged as a Claude
plugin (`.claude-plugin/plugin.json` plus a marketplace entry, installed with
`/plugin install`) so several projects share one updated copy. A plugin cannot
carry `CLAUDE.md` or permissions. Hook commands in a plugin use
`${CLAUDE_PLUGIN_ROOT}`, which is an error in a `settings.json` hook, and plugin
skills are invoked as `/<plugin>:<skill>`, so serving both forms from the same
files needs small changes (an open question, tracked as issue #19). This repository
therefore ships as copyable files for now.

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

Seven skills only run when you type them (Claude cannot start them itself):

| Command | Example |
|---|---|
| `/new-feature <description or issue #>` | `/new-feature Show an empty state when the order list has no items` |
| `/review-pr [PR number] [aspects] [parallel]` | `/review-pr` · `/review-pr errors tests` · `/review-pr 12 parallel` |
| `/setup-issue-tracker` | writes the tracker, label and domain-docs configuration for a repository |
| `/triage [issue #]` | `/triage Let's look at #42` moves an issue through the triage states and writes an agent brief |
| `/run-github-project <mode>` | `review`, `setup`, `next` (one ticket) or `drain` (until the board is empty); asks before merging |
| `/wayfinder <idea or map #>` | charts a map of decision tickets, then resolves them one at a time |
| `/implement-with-subagents` | delegated implementation of supplied tickets |

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
| `implement-with-subagents` | Delegated implementation of supplied tickets (user-typed) |
| `run-github-project` | Operate a GitHub Project board workflow: `next` delivers one ticket, `drain` runs the board until empty (user-typed) |
| `triage`, `wayfinder`, `setup-issue-tracker` | Issue triage, large-work planning as a map of decision tickets, tracker and label setup (user-typed) |

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
- Thresholds: cyclomatic complexity ≤ 15, function ≤ 60 lines, class ≤ 600 lines, ≤ 6 parameters, nesting ≤ 4, line coverage domain ≥ 80% / data ≥ 70% / other modules ≥ 60% (an overall figure needs a root aggregation project).
- Folder, package and file naming (package mirrors path, file named after its main type, role suffixes, split at ~15 files) is in `references/structure-and-naming.md`.
- Quality-gate templates (Spotless and ktlint, detekt, Kover, convention plugins) are in `references/quality-gates.md`.

## GitHub and pull requests

Prerequisites: a GitHub `origin`, `gh auth login`, and CI that runs the Gradle
checks. The everyday flow:

1. create an issue, 2. `/to-plan #12`, 3. `deliver spec #12 as a pull request`,
4. `/review-pr 12`, 5. `shepherd PR #12`, 6. merge only when you say so.

Details and optional triage and project-board workflows: [`.claude/pr-workflow.md`](.claude/pr-workflow.md).

Optional issue-tracker and board setup (run once per repository, user-typed):

1. `/setup-issue-tracker` writes `docs/agents/issue-tracker.md`, `triage-labels.md` and `domain.md` and the `## Agent skills` block in `CLAUDE.md`.
2. `/triage` moves issues through `needs-triage` to `ready-for-agent` with an agent brief.
3. `/run-github-project Set up the binding for <project URL>` writes `docs/agents/run-github-project.md` (Status lanes, Priority, labels, merge policy). It needs the `gh` token `project` scope (`gh auth refresh -h github.com -s project`). Humans move issues from Backlog to Todo; the skill never works Backlog.

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
docs/agents/              issue tracker, triage labels, domain docs, GitHub Project binding
docs/evals/               skill-selection check: prompts, expected skills and results
GLOSSARY.md               vocabulary of the environment check (Tool, Status, OK, UPDATE...)
LICENSE                   MIT
```

## Status

Checked on Windows 11 with Git Bash, Gradle 9.8.2, AGP 9.4.1, Kotlin 2.4.21, on small
projects built for each test (none of them is kept in this repository):

| Area | State |
|---|---|
| `setup-environment` check, `gradle-run`, `build-and-verify`, `run-on-device` on an emulator | tested |
| `kotlin-architecture`, Kotlin and Compose skills, `kotlin-reviewer` and the four review agents, `android-verifier` | tested (the quality-gate templates were rebuilt from the doc and run on real projects; see the rows below) |
| Format hook with ktlint, kotlin-lsp (symbols, cross-module definitions, hover) | tested |
| `to-plan`, `deliver-spec`, `shepherd`, with CI, two independent reviews and a squash merge | tested on a real GitHub repository |
| `/new-feature` and `/review-pr` | tested end to end when typed by the user (Claude is blocked from invoking them itself) |
| `setup-issue-tracker`, `triage` (issues #8 and #9 triaged with agent briefs), `run-github-project` `review` and `setup` (binding for Project #2, validated live, merged in PR #12) | tested on a real repository and board |
| `run-github-project` `next` (issue #8 to PR #15: claim, fast-path plan, implementation, independent review and repair, CI, merge, close, board to Done) | tested on a real repository and board |
| `run-github-project` `drain` (issue #9 to PR #17: claim, full `to-plan` plan, implementation, independent review and repair, CI, watcher wait at the merge gate, merge, close, board to Done) | tested on a real repository and board |
| `wayfinder` (two real maps, #19 and #31: charting, parallel research agents, resolving research and live grilling tickets with the maintainer's answers, the live blocking gate, closing a finished map) | tested on a real repository |
| `CHECK_ENV_PIN` (pinned tools in the environment check), `test_check_env.sh` with 22 cases | tested on Windows Git Bash only |
| The quality-gate templates in `quality-gates.md` (convention plugin, Spotless, detekt 2.x and 1.x, Compose rules, Kover per-module and overall bounds, API check, module rule) | rebuilt from the doc and run on a multi-module JVM project (every gate under `check`, each red when violated), an Android Compose project, product flavors, and a KMP module with Compose Multiplatform; detekt 1.x on Temurin 17 and 21 in Actions. iOS targets were not built (Windows host) |
| The CI template (GitHub Actions) in `quality-gates.md`, with its SHA-pinned actions and Dependabot block | run on GitHub Actions on a JVM, an Android (`check assembleDebug`) and a KMP project (`ubuntu-latest` and `macos-latest`) with the current pins, green on all; red with a broken test on the earlier `v4` pins. The Dependabot block follows GitHub's documentation but was not run (it only takes effect on a default branch) |
| Skill selection: do agents load the right skill from a plain request? 32 realistic prompts (Kotlin and Compose, architecture, engineering practice, Android CLI and device flows, official Android skills, delegated-worker briefs, negative controls) | 31 of 32 hit the expected skill on the first run, no wrong picks; the one miss (an Android documentation lookup answered partly from memory) was fixed in `CLAUDE.md` and re-checked. Prompts and results: [`docs/evals/skill-selection.md`](docs/evals/skill-selection.md) |
| Physical Android phone (Samsung, Android 15): install, launch, full UI flow, logs | tested |
| macOS and Linux for the repository's own scripts (`check-env.sh`, `gradle-run`, the format hook) | not yet tested (written to be portable); the CI template itself did run on `ubuntu-latest` and `macos-latest` |

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
| A tool is always `UPDATE` but you want to stay on its version | Set `CHECK_ENV_PIN=gh,ktlint` (comma-separated, case-insensitive); pinned tools show `OK` with a `pinned:` note |

## License

[MIT](LICENSE). Copyright (c) 2026 Ray Shen.
