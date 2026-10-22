# Skill-selection check

Do agents pick the right skill from a plain request? This is the prompt set and the result of the
first run (October 2026). Re-run it after changing any skill `description`, adding a skill, or
changing the routing rules in `CLAUDE.md`.

## How it was run

Each case ran in a fresh agent in this repository with only the request below, no hint about which
skill to use, and a dry-run instruction (no edits, no builds). The agent had to end its reply with
a `SKILLS_LOADED:` line listing the skills it loaded with the Skill tool. Expected skills were fixed
before running. A hit means the expected skill was among the loaded skills; a wrong pick means a
loaded skill that does not belong to the request.

## Result: 31 of 32 hits, 0 wrong picks

| # | Request (short) | Expected | Loaded |
|---|---|---|---|
| A1 | ViewModel collects a Flow in `init`, stale data after rotation | `kotlin-concurrency-and-flow` | router, `kotlin-concurrency-and-flow` |
| A2 | `when` over a sealed type with an `else` branch | `kotlin-control-flow` | router, `kotlin-control-flow` |
| A3 | Public API of a `Money` type in a library module | `kotlin-api-design` | `kotlin-api-design` |
| A4 | Text field state in the ViewModel, whole screen recomposes | `compose-state-and-effects` | `compose-state-and-effects` |
| A5 | LazyColumn recomposes every item on scroll | `compose-performance` | router, `compose-performance` |
| A6 | Reusable `PrimaryButton` signature | `compose-component-design` | router, `compose-component-design` |
| A7 | Expand-to-detail animation | `compose-animations` | `compose-animations` |
| A8 | Compose UI test for a login error | `compose-ui-testing-patterns` | `compose-ui-testing-patterns` |
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
| D3 | Build `:core:domain` and report whether tests pass | `build-and-verify` | `build-and-verify`, `gradle-run` |
| D4 | List devices with the `android` tool | `android-cli` | `android-cli` |
| D5 | Current foreground-service type requirements for targetSdk 35 | `android-cli` (docs lookup) | **none** (used `android docs`, part of the answer from memory) |
| E1 | Migrate to Jetpack Navigation 3 | `navigation-3` | `navigation-3` |
| E2 | Edge-to-edge and insets on Android 15 | `edge-to-edge` | `edge-to-edge` |
| E3 | Upgrade AGP 8.7 to 9 | `agp-9-upgrade` | `agp-9-upgrade` |
| E4 | Shrink broad R8 keep rules | `r8-analyzer` | `r8-analyzer` |
| E5 | Audit exported components and permissions | `android-permissions-security` / `android-intent-security` | both |
| F1 | Python script to rename files | none | none |
| F2 | Explain SQL window functions | none | none |
| G1 | Delegated ticket: debounced search in a ViewModel | router, then `kotlin-concurrency-and-flow` | `deliver-spec`, router, `kotlin-concurrency-and-flow` |
| G2 | Delegated ticket: expandable FAQ composable with a test | router, then Compose skills | `deliver-spec`, router, `compose-component-design`, `compose-animations`, `compose-ui-testing-patterns`, `kotlin-architecture` |

## Notes

- The Kotlin router (`kotlin-skill-router`) was used on 5 of 8 Kotlin and Compose requests and on
  both delegated-worker briefs; the other Kotlin and Compose requests went straight to the owning
  skill.
- D5 is the only miss. The agent followed the `CLAUDE.md` rule to use `android docs search`, but did
  not load `android-cli` and filled part of the answer from memory. If this recurs, strengthen the
  `CLAUDE.md` Android line.
- One G2 run first omitted the required `SKILLS_LOADED:` line and was re-run; the result above is
  the re-run.
- This is a single run per case on one machine and one model. Treat it as a regression check, not a
  statistical guarantee.
