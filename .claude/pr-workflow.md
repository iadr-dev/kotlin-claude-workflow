# PR workflow (GitHub)

Status: **untested.** These skills were copied in but never run against a real
repository. Treat the first run as a trial and watch what each step pushes.

## Prerequisites

1. A git repo with a GitHub `origin` (`git remote get-url origin`).
2. `gh auth status` shows you logged in (`gh auth login` otherwise).
3. CI that runs the project checks on every PR (GitHub Actions; see the
   [CI template (GitHub Actions)](skills/kotlin-architecture/references/quality-gates.md#ci-template-github-actions)).
   The skills use CI as confirmation, and `to-plan` proves baselines from green
   CI. Ask Claude to create the workflow file if the repo has none.
4. `bash .claude/skills/setup-environment/scripts/check-env.sh .` is all OK.

## Everyday flow (start here)

| Step | You type | What happens |
|---|---|---|
| 1. Issue | create it on GitHub with acceptance criteria | the spec |
| 2. Plan | `/to-plan #12` (add `--auto` to skip questions) | repo-aware plan; stops if the issue is incomplete |
| 3. Deliver | `deliver spec #12 as a pull request` | implements, validates, independent review, opens the PR |
| 4. Extra review | `/review-pr 12` | kotlin-reviewer plus error/test/type/comment agents on that PR |
| 5. Follow-up | `shepherd PR #12` | polls CI and review comments, fixes, resolves threads |
| 6. Merge | `merge PR #12 when CI is green` | **only on your explicit words**; shepherd never merges on its own |

Small change without an issue: `/new-feature <description>`, answer `PR` at the
Ship question (uses `/commit-push-pr`), then continue at step 4.

## Optional: issue triage and project board

- `/setup-issue-tracker` once per repo (tracker, labels, domain docs) before
  using `triage` or `wayfinder`.
- `triage`: move incoming issues through states and write agent-ready briefs.
- `wayfinder`: map work too large for one session as decision tickets.
- `run-github-project`: operate a GitHub Project board end to end. Heavy; try
  only after the everyday flow feels right. Start with its review/setup mode,
  which needs nothing else.

## Guardrails already in `.claude/settings.json`

- `git push` always asks first.
- Nothing merges without explicit authority in the invocation.
