# Skill-selection check

Do agents pick the right skill from a plain request? This is the prompt set, the method and the
result of the first clean run (October 2026). Re-run it after changing any skill `description`,
adding a skill, or changing the routing rules in `CLAUDE.md`:

```sh
python3 docs/evals/run_skill_eval.py            # all 33 cases against this repository
python3 docs/evals/run_skill_eval.py A1 D5      # some cases
python3 docs/evals/run_skill_eval.py --fixture G1 G2   # worker briefs against a small Kotlin fixture
```

## Method

Each case runs as a real headless Claude Code session (`claude -p`) in this repository, given only
the plain request below. The prompt never mentions skills, dry runs or reporting, so the session
behaves as it would for a normal user. Edits are blocked; only read-only tools and `android docs`
are allowed. The skills loaded are read from the session's tool-call log (stream-json), not from
the agent's own account. Expected skills were fixed before the run.

Criteria:

- **Hit** (most cases): at least one expected skill was loaded. Negative controls (F1, F2) must load nothing.
- **Hit for the documentation questions (D5, D6)**: an expected skill was loaded **or** the session ran
  `android docs fetch`. The goal there is an answer from fetched pages, not from memory; the agent
  may call the CLI directly, as `CLAUDE.md` tells it to, without loading the `android-cli` skill.
  This criterion was widened after the first clean run, which showed that behaviour. The strict
  count is shown below as well.
- **Worker briefs (G1, G2)** need real Kotlin code to work on, so they run with `--fixture` (a tiny
  project with a `SearchViewModel`, a `SearchRepository` and a `:core:designsystem` module). In this
  repository, which has no Kotlin code, a worker correctly stops after loading the router and
  `deliver-spec`.

## Result: 33 of 33 on behaviour, 31 of 33 on the strict skill count, no wrong picks

| # | Request (short) | Expected | Loaded, in order |
|---|---|---|---|
| A1 | ViewModel collects a Flow in `init`, stale data after rotation | `kotlin-concurrency-and-flow` | router, `kotlin-concurrency-and-flow` |
| A2 | `when` over a sealed type with an `else` branch | `kotlin-control-flow` | router, `kotlin-control-flow` |
| A3 | Public API of a `Money` type in a library module | `kotlin-api-design` | router, `kotlin-api-design` |
| A4 | Text field state in the ViewModel, whole screen recomposes | `compose-state-and-effects` | router, `compose-state-and-effects`, `compose-performance` |
| A5 | LazyColumn recomposes every item on scroll | `compose-performance` | router, `compose-performance` |
| A6 | Reusable `PrimaryButton` signature | `compose-component-design` | router, `compose-component-design` |
| A7 | Expand-to-detail animation | `compose-animations` | router, `compose-animations` |
| A8 | Compose UI test for a login error | `compose-ui-testing-patterns` | router, `compose-ui-testing-patterns` |
| B1 | Where do the repository interface and implementation live | `kotlin-architecture` | `kotlin-architecture` |
| B2 | Create a use case for `getUser(id)`? | `kotlin-architecture` | `kotlin-architecture` |
| B3 | `managers` / `utils` / `helpers` packages, naming for a large project | `kotlin-architecture` | `kotlin-architecture` |
| B4 | Set up quality gates (format, static analysis, coverage) | `kotlin-architecture` | `kotlin-architecture` |
| B5 | `:feature:orders` depends on `:feature:payments` | `kotlin-architecture` | `kotlin-architecture` |
| C1 | Build a discount calculator test-first | `tdd` | `tdd` |
| C2 | Intermittent CI-only test failure | `diagnosing-bugs` | `diagnosing-bugs` |
| C3 | Interview me about an offline-sync plan | `grilling` | `grilling` |
| C4 | Order / purchase / checkout used interchangeably, write a glossary | `domain-modeling` | `domain-modeling` |
| C5 | Throwaway checkout-screen prototype | `prototype` | `prototype` |
| D1 | Run the app on the emulator and screenshot the login screen | `run-on-device` | `run-on-device` |
| D2 | New laptop: is everything installed and up to date | `setup-environment` | `setup-environment` |
| D3 | Build `:core:domain` and report whether tests pass | `build-and-verify` or `gradle-run` | `gradle-run` |
| D4 | List devices with the `android` tool | `android-cli` | `android-cli` |
| D5 | Current foreground-service type requirements for targetSdk 35 | `android-cli`, or a docs fetch | no skill; searched and fetched docs pages in 3 of 3 runs |
| D6 | Exact-alarm permissions on Android 14 vs Android 12 | `android-cli`, or a docs fetch | no skill; searched and fetched 3 to 4 pages in 3 of 3 runs |
| E1 | Migrate to Jetpack Navigation 3 | `navigation-3` | `navigation-3` |
| E2 | Edge-to-edge and insets on Android 15 | `edge-to-edge` | `edge-to-edge`, router |
| E3 | Upgrade AGP 8.7 to 9 | `agp-9-upgrade` | `agp-9-upgrade` |
| E4 | Shrink broad R8 keep rules | `r8-analyzer` | `r8-analyzer` |
| E5 | Audit exported components and permissions | `android-permissions-security` or `android-intent-security` | both |
| F1 | Python script to rename files | none | none |
| F2 | Explain SQL window functions | none | none |
| G1 | Delegated ticket: debounced search in a ViewModel (fixture) | router, then `kotlin-concurrency-and-flow` | router, `deliver-spec`, `kotlin-concurrency-and-flow` |
| G2 | Delegated ticket: expandable FAQ composable with a test (fixture) | router, then Compose skills | router, `deliver-spec`, `compose-animations`, `compose-component-design`, `compose-ui-testing-patterns`, `kotlin-architecture` |

## Notes

- `router` is `kotlin-skill-router`. In this run it ran first on all eight Kotlin and Compose
  requests (A1 to A8) and on both worker briefs, then the owning skill loaded.
- D5 and D6 never load the `android-cli` skill, but every one of the six runs searched the Android
  docs, fetched real pages and said which parts it could not check. That follows the `CLAUDE.md`
  line "Answer Android platform questions from the fetched pages, not from memory or search
  snippets". Before that sentence existed, a first instrumented run answered part of D5 from memory.
- An earlier first run used sub-agents that were told it was a dry run and asked to report their
  skills. That was friendlier than real use and is superseded by this run.
- One session per case (three for D5 and D6). Model behaviour varies between runs, so treat this as
  a regression check, not a guarantee. The `prototype` skill creates an empty `prototype/` folder
  when it runs; delete it after running C5.
