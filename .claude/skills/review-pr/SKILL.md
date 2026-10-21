---
name: review-pr
description: Multi-agent review of the current diff or a GitHub PR for this Kotlin project - correctness/architecture, error handling, tests, type design, and comments - aggregated into one prioritized action list.
disable-model-invocation: true
argument-hint: "[PR number] [aspects: code errors tests types comments all] [parallel]"
---

# Review PR: $ARGUMENTS

## 1. Scope

- PR number given → `gh pr diff <n> --name-only` and `gh pr view <n>`; review that diff.
- Otherwise → `git diff --name-only HEAD` (plus staged). Nothing changed → say so and stop.

## 2. Pick agents

| Aspect | Agent | Runs when |
|---|---|---|
| `code` | `kotlin-reviewer` | always |
| `errors` | `silent-failure-hunter` | diff touches `try/catch`, `runCatching`, `Result`, `Flow.catch`, fallbacks, retries, coroutine launches |
| `tests` | `pr-test-analyzer` | behavior changed (with or without test changes) |
| `types` | `type-design-analyzer` | new or changed `class`/`data class`/`value class`/`sealed`/`interface` |
| `comments` | `comment-analyzer` | KDoc or substantial comments added/changed |

Explicit aspects in the arguments override the defaults; `all` runs every agent.
Tell each agent the exact files/diff range and that the project's rules are in
`CLAUDE.md` and the `kotlin-architecture` skill.

Run agents in parallel (default) unless the user asked for sequential.

## 3. Aggregate

Deduplicate findings reported by several agents (keep the most specific).
Drop findings without a concrete file:line and failure scenario.

```markdown
# Review summary

## Critical (must fix before merge)
- [agent] description — path/File.kt:LINE

## Important (should fix)
- ...

## Suggestions
- ...

## Strengths
- ...

## Next steps
1. Fix critical issues, then re-run `build-and-verify`
2. Re-run `/review-pr <aspects>` for the touched aspects
```

Polish/simplification after the review: the built-in `/simplify`.
Posting comments to the PR: only when the user asks (`gh pr review` / `gh pr comment`).
