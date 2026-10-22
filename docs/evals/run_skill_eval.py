#!/usr/bin/env python3
"""Skill-selection check: run each plain user request in a real headless Claude Code session and read
the Skill tool calls from the stream-json log. The prompts never mention skills, dry runs or reporting.

Usage (from anywhere, needs the `claude` CLI on PATH):
    python3 docs/evals/run_skill_eval.py                 # all cases against this repository
    python3 docs/evals/run_skill_eval.py A1 D5           # only some cases
    python3 docs/evals/run_skill_eval.py --fixture G1 G2 # worker briefs against a small Kotlin fixture

Edits are blocked (Edit, Write, NotebookEdit, Agent denied); only read-only tools and `android docs`
are allowed. Each run gets a private temporary directory (tempfile.mkdtemp, mode 0700) that holds the
logs and, with --fixture, the fixture project. Results are printed as a table.
"""
import json, os, re, shutil, subprocess, sys, tempfile, time
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
CLAUDE = shutil.which("claude")

# id: (prompt, expected skills (any one is a hit), kind)
#   kind "skill": hit if an expected skill was loaded (empty set = no skill may be loaded)
#   kind "docs":  hit if an expected skill was loaded OR the session ran `android docs fetch`
CASES = {
 "A1": ("Our ViewModel starts collecting a Flow in init and after a screen rotation the UI shows stale data. How should we expose and collect that Flow?", {"kotlin-concurrency-and-flow"}, "skill"),
 "A2": ("I have `sealed interface Result { data class Ok(val v: Int): Result; data class Err(val e: Throwable): Result; object Loading: Result }` and a `when (r) { is Result.Ok -> show(r.v); else -> hideSpinner() }`. Is the else branch a problem?", {"kotlin-control-flow"}, "skill"),
 "A3": ("I'm designing the public API of a small Kotlin library module: a Money type with amount and currency, plus a parse function. Should Money be a value class, a data class, or something else, and where should the parse function live?", {"kotlin-api-design"}, "skill"),
 "A4": ("In our Jetpack Compose screen the text field value lives in the ViewModel as a StateFlow and every keystroke recomposes the whole screen and sometimes loses the cursor. How should state be owned and the flow collected in the composable?", {"compose-state-and-effects"}, "skill"),
 "A5": ("Our LazyColumn of product cards janks while scrolling and the Layout Inspector shows every item recomposing on each scroll frame. What do we check and change?", {"compose-performance"}, "skill"),
 "A6": ("Design a reusable PrimaryButton composable for our design system: it needs a label, an optional leading icon, a loading state and to be usable in different layouts. What should its signature look like?", {"compose-component-design"}, "skill"),
 "A7": ("I want an animated transition when a list item expands into a detail card in Jetpack Compose, with the content fading and the size animating. Which animation APIs should I use?", {"compose-animations"}, "skill"),
 "A8": ("Write a Compose UI test for the login screen that checks the error text appears after a failed login. How do I structure it and find the nodes reliably?", {"compose-ui-testing-patterns"}, "skill"),
 "B1": ("In our multi-module Android app, where should the OrdersRepository interface and its implementation live, and which module should depend on which?", {"kotlin-architecture"}, "skill"),
 "B2": ("Our ViewModel only calls repository.getUser(id). Should we create a GetUserUseCase for it, or is that overkill?", {"kotlin-architecture"}, "skill"),
 "B3": ("Our new Kotlin module has packages com.shop.managers, com.shop.utils and com.shop.helpers with big files like OrderUtils.kt. The project will grow to hundreds of files. Is this layout ok, and how should we name folders, packages and files?", {"kotlin-architecture"}, "skill"),
 "B4": ("Set up code quality gates for our new Kotlin project: formatting, static analysis and coverage thresholds that fail CI. What do we add?", {"kotlin-architecture"}, "skill"),
 "B5": ("In code review I noticed :feature:orders has implementation(project(':feature:payments')) so it can reuse a payment form. Is that acceptable? What should we do instead?", {"kotlin-architecture"}, "skill"),
 "C1": ("Let's build a discount calculator test-first: 10% off over 100, 20% off over 500. Walk me through how we should proceed.", {"tdd"}, "skill"),
 "C2": ("A unit test fails only about one run in ten on CI and never locally. I can't reproduce it. How do we find the root cause?", {"diagnosing-bugs"}, "skill"),
 "C3": ("I have a rough plan for adding offline sync to our app. Interview me about it until we have a shared understanding; ask me the questions you need.", {"grilling"}, "skill"),
 "C4": ("Our team uses 'order', 'purchase' and 'checkout' interchangeably and it causes bugs. Help us define the domain terms and write them down as a glossary.", {"domain-modeling"}, "skill"),
 "C5": ("Before we commit to a design, build me a throwaway prototype of the checkout screen so we can react to how it looks and feels.", {"prototype"}, "skill"),
 "D1": ("Run the app on the emulator and take a screenshot of the login screen.", {"run-on-device"}, "skill"),
 "D2": ("I just got a new laptop. Check that everything I need for Android and Kotlin development is installed and up to date.", {"setup-environment"}, "skill"),
 "D3": ("I changed the :core:domain module. Build it and tell me whether the tests pass.", {"build-and-verify", "gradle-run"}, "skill"),
 "D4": ("How do I use the android command-line tool to list my connected devices and see what it can do?", {"android-cli"}, "skill"),
 "D5": ("What are the current Android requirements for declaring foreground service types on a targetSdk 35 app?", {"android-cli"}, "docs"),
 "D6": ("What permission and manifest declarations does an Android 14 app need to schedule exact alarms, and what changed compared with Android 12?", {"android-cli"}, "docs"),
 "E1": ("We want to migrate our Compose app from Navigation 2 to Jetpack Navigation 3. How should we approach it?", {"navigation-3"}, "skill"),
 "E2": ("On Android 15 our Compose screens are drawn under the status bar and the bottom buttons are hidden behind the navigation bar. How do we handle edge-to-edge and insets properly?", {"edge-to-edge"}, "skill"),
 "E3": ("Upgrade our Android project from Android Gradle Plugin 8.7 to AGP 9. What do we need to change?", {"agp-9-upgrade"}, "skill"),
 "E4": ("Our release APK is big and our R8 keep rules have grown over the years with lots of broad -keep entries. Analyze and shrink them.", {"r8-analyzer"}, "skill"),
 "E5": ("Audit our AndroidManifest for exported activities, receivers and services and for over-broad permissions. What should we look for and fix?", {"android-permissions-security", "android-intent-security"}, "skill"),
 "F1": ("Write me a short Python script that renames all .jpg files in a folder to their creation date, like 2026-10-22_1.jpg.", set(), "skill"),
 "F2": ("Explain SQL window functions with a small example: running total per customer.", set(), "skill"),
 "G1": ("You are the ticket owner of a delegated implementation task in an isolated worktree. The controller's packet says: \"Approved ticket: add a debounced search in SearchViewModel (300 ms) that emits results from SearchRepository.search(query) as a Flow and cancels in-flight searches when the query changes. Obey the repository instructions. Own the worktree, branch and local commits only.\" Start the task.", {"kotlin-concurrency-and-flow"}, "skill"),
 "G2": ("You are the ticket owner of a delegated implementation task in an isolated worktree. The controller's packet says: \"Approved ticket: add an expandable FAQ item composable (title, animated expand/collapse of the answer) to the :core:designsystem module, with a screenshot-free unit/UI test. Obey the repository instructions. Own the worktree, branch and local commits only.\" Start the task.", {"compose-animations", "compose-component-design", "compose-ui-testing-patterns"}, "skill"),
}

# Read-only tools only. Prefix rules such as `git log:*` are NOT safe (git accepts abbreviated options
# like --out=<file>, so a prefix rule can write files), hence exact forms for git.
ALLOWED = ["Skill", "Read", "Grep", "Glob", "Bash(android docs:*)", "Bash(android --help:*)", "Bash(android help:*)",
           "Bash(git status)", "Bash(git status --short)", "Bash(ls:*)"]
DENIED = ["Edit", "Write", "NotebookEdit", "Agent"]


def build_fixture(dest):
    """A tiny Kotlin project (with this repository's .claude and CLAUDE.md) for the worker briefs."""
    shutil.rmtree(dest, ignore_errors=True)
    os.makedirs(dest)
    shutil.copytree(os.path.join(ROOT, ".claude"), os.path.join(dest, ".claude"),
                    ignore=shutil.ignore_patterns("settings.local.json"))
    for f in ("CLAUDE.md", "GLOSSARY.md"):
        shutil.copy(os.path.join(ROOT, f), dest)
    files = {
        "settings.gradle.kts": 'rootProject.name = "shop"\ninclude(":core:domain", ":core:designsystem", ":feature:search")\n',
        "core/domain/build.gradle.kts": 'plugins { kotlin("jvm") version "2.4.21" }\n',
        "core/designsystem/build.gradle.kts": 'plugins { kotlin("jvm") version "2.4.21" }\n',
        "feature/search/build.gradle.kts": 'plugins { kotlin("jvm") version "2.4.21" }\n',
        "core/domain/src/main/kotlin/com/shop/domain/SearchRepository.kt":
            "package com.shop.domain\n\nimport kotlinx.coroutines.flow.Flow\n\ninterface SearchRepository {\n    fun search(query: String): Flow<List<String>>\n}\n",
        "feature/search/src/main/kotlin/com/shop/search/SearchViewModel.kt":
            "package com.shop.search\n\nimport androidx.lifecycle.ViewModel\nimport com.shop.domain.SearchRepository\nimport kotlinx.coroutines.flow.MutableStateFlow\n\nclass SearchViewModel(private val repository: SearchRepository) : ViewModel() {\n    val query = MutableStateFlow(\"\")\n    fun onQueryChanged(q: String) { query.value = q }\n}\n",
        "core/designsystem/src/main/kotlin/com/shop/designsystem/Theme.kt":
            "package com.shop.designsystem\n\nimport androidx.compose.runtime.Composable\n\n@Composable\nfun ShopTheme(content: @Composable () -> Unit) = content()\n",
    }
    for rel, text in files.items():
        path = os.path.join(dest, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
    subprocess.run(["git", "init", "-q", "."], cwd=dest, check=True)
    subprocess.run(["git", "add", "-A"], cwd=dest, check=True, capture_output=True)
    subprocess.run(["git", "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "fixture"],
                   cwd=dest, check=True, capture_output=True)


def run(args):
    cid, prompt, repo, out = args
    path = os.path.join(out, cid + ".jsonl")
    try:
        with open(path, "w", encoding="utf-8") as fh:
            p = subprocess.run([CLAUDE, "-p", prompt, "--output-format", "stream-json", "--verbose",
                                "--no-session-persistence", "--max-turns", "20",
                                "--allowedTools", *ALLOWED, "--disallowedTools", *DENIED],
                               cwd=repo, stdin=subprocess.DEVNULL, stdout=fh, stderr=subprocess.PIPE,
                               timeout=600, text=True)
        return cid, p.returncode
    except subprocess.TimeoutExpired:
        return cid, "timeout"


def parse(path):
    skills, fetches = [], 0
    for line in open(path, encoding="utf-8", errors="replace"):
        line = line.strip()
        if not line:
            continue
        try:
            e = json.loads(line)
        except Exception:
            continue
        if e.get("type") != "assistant":
            continue
        for c in e.get("message", {}).get("content", []):
            if c.get("type") != "tool_use":
                continue
            if c["name"] == "Skill":
                skills.append(c.get("input", {}).get("skill"))
            elif c["name"] == "Bash":
                fetches += len(re.findall(r"android docs fetch", c.get("input", {}).get("command", "")))
    return skills, fetches


def main(argv):
    if not CLAUDE:
        sys.exit("the `claude` CLI is not on PATH")
    use_fixture = "--fixture" in argv
    ids = [a for a in argv if not a.startswith("--")]
    cases = [(k, v) for k, v in CASES.items() if not ids or k in ids]
    out = tempfile.mkdtemp(prefix="skill-eval-")  # private (0700) and unpredictable
    repo = ROOT
    if use_fixture:
        repo = os.path.join(out, "fixture")
        build_fixture(repo)
    with ThreadPoolExecutor(max_workers=8) as ex:
        runs = dict(ex.map(run, [(k, v[0], repo, out) for k, v in cases]))
    hits = 0
    print("id  verdict  expected -> loaded (in order) | docs fetches | exit")
    for cid, (prompt, expected, kind) in cases:
        skills, fetches = parse(os.path.join(out, cid + ".jsonl"))
        got = set(skills)
        if not expected:
            ok = not got
        elif kind == "docs":
            ok = bool(got & expected) or fetches >= 1
        else:
            ok = bool(got & expected)
        hits += ok
        print(f"{cid:3} {'HIT ' if ok else 'MISS'}  {','.join(sorted(expected)) or 'none'} -> "
              f"{', '.join(skills) or 'none'} | {fetches} | {runs[cid]}")
    print(f"HITS {hits}/{len(cases)}   logs: {out}")


if __name__ == "__main__":
    main(sys.argv[1:])
