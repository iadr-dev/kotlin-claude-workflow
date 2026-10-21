---
name: new-feature
description: End-to-end feature workflow for this Kotlin project (Android, KMP, or JVM) - plan, implement with the right Kotlin/Compose skills, test, build, verify on a device, review, then commit or open a PR.
disable-model-invocation: true
argument-hint: "<feature description or GitHub issue number>"
---

# New feature: $ARGUMENTS

Work through the phases in order. Pause for the user only where marked **[ask]**.

## 1. Understand

- GitHub issue number given → `gh issue view <n>` for the spec.
- Read the modules the feature touches and their existing patterns
  (state holders, DI, navigation, error types). Use the `feature-dev:code-explorer`
  agent if the area is unfamiliar or spans many modules.
- Detect the project shape as in `build-and-verify` step 1.

## 2. Plan  **[ask]**

- Non-trivial or multi-module work → use `to-plan`.
- Small change → a short bullet plan: files to change, new types, tests to add, how it will be verified (unit test / device flow / endpoint call).
- Either way, load `kotlin-architecture` and answer its §6 scalability checklist in the plan: owning layer/module for each new type, dependency direction, new modules (if any), public surface.

Show the plan and wait for approval.

## 3. Implement

Before writing code, route through `kotlin-skill-router` and load the
skill that owns each decision, typically:

| Change involves | Skill |
|---|---|
| Layer placement, new module, DI wiring, code standards, metrics | `kotlin-architecture` |
| Coroutines, Flow, StateFlow/SharedFlow, cancellation | `kotlin-concurrency-and-flow` |
| `when`, sealed types, nullability branching | `kotlin-control-flow` |
| Public functions, value/data classes, expect/actual | `kotlin-api-design` |
| Compose state, effects, ViewModel collection | `compose-state-and-effects` |
| New reusable composable | `compose-component-design` |
| Motion | `compose-animations` |
| Android platform area (navigation, edge-to-edge, adaptive, camera...) | matching Android skill; check `android docs search "<query>"` (then `android docs fetch kb://...`) for current APIs |
| Third-party library API | context7 docs |

Write tests alongside the code. For behavior with a clear seam use `tdd`;
for Compose UI use `compose-ui-testing-patterns`. Hard bug along the way →
`diagnosing-bugs`.

## 4. Verify

1. `build-and-verify` on the touched modules. Fix until green.
2. User-visible Android/desktop change → `run-on-device` (physical device if
   the feature depends on hardware). Server change → call the endpoint.

## 5. Review

Launch the `kotlin-reviewer` agent on the diff. Fix confirmed issues, re-run
step 4 for anything that changed. Mention dismissed findings and why.

## 6. Ship  **[ask]**

Ask whether to commit only, or open a PR.

- Commit → `/commit` (commit-commands).
- PR → `deliver-spec` when working from an approved issue/spec, otherwise
  `/commit-push-pr`. After opening, `shepherd` can follow CI and review comments
  if the user wants.

Final message: what changed, what was verified (tasks run, device used), what was not verified.
