#!/usr/bin/env bash
# Self-contained test for check-env.sh (CHECK_ENV_PIN). Offline-safe: stand-in executables are placed
# first on PATH, so no real tool is downgraded and nothing touches the network.
# Usage: bash test_check_env.sh
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="$here/check-env.sh"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
stubs="$tmp/stubs"; toolbox="$tmp/toolbox"; proj="$tmp/project"
mkdir -p "$stubs" "$toolbox" "$proj/gradle/wrapper" "$tmp/sdk"

stub() { # dir name body
  printf '#!/usr/bin/env bash\n%s\n' "$3" > "$1/$2"; chmod +x "$1/$2"
}
# Versions come from env so a case can change them: STUB_GH, STUB_JAVA.
stub "$stubs" git     'echo "git version 2.50.0"'
stub "$stubs" java    'echo "openjdk version \"${STUB_JAVA:-21.0.1}\" 2025-01-01" >&2'
stub "$stubs" python3 'echo "Python 3.12.0"'
stub "$stubs" gh      'echo "gh version ${STUB_GH:-2.102.0} (2025-01-01)"'
stub "$stubs" adb     'echo "Android Debug Bridge version 1.0.41"; echo "Version 36.0.0-1"'
stub "$stubs" ktlint  'echo "1.3.0"'
stub "$stubs" android '[ "$1" = "--version" ] && echo "0.7.0"; exit 0'
stub "$stubs" winget  'exit 1'
for t in ktfmt kotlin-lsp kotlin-lsp.cmd intellij-server claude; do stub "$stubs" "$t" 'exit 1'; done
# curl: only the lookups below answer; everything else fails (latest unknown).
stub "$stubs" curl 'for a in "$@"; do case "$a" in
  */cli/cli/releases/latest) printf "https://github.com/cli/cli/releases/tag/v2.103.0"; exit 0 ;;
  */ktlint/ktlint/releases/latest) printf "https://github.com/ktlint/ktlint/releases/tag/1.3.0"; exit 0 ;;
  *services.gradle.org/versions/current*) printf "{\"version\":\"9.0\"}"; exit 0 ;;
esac; done; exit 22'

# Project toolchain: a Gradle wrapper one major behind the stubbed latest (9.0).
echo 'distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip' \
  > "$proj/gradle/wrapper/gradle-wrapper.properties"

# Wrappers (not copies) so the real binaries keep their library path, e.g. msys DLLs on Windows.
stub_exec() { printf '#!/bin/sh\nexec "%s" "$@"\n' "$3" > "$1/$2"; chmod +x "$1/$2"; }
# Private toolbox for the MISSING case: only the real tools check-env.sh needs, plus the stubs minus python3.
for t in env bash sed grep awk cut tr sort head uname timeout cat; do
  p="$(command -v "$t")" && stub_exec "$toolbox" "$t" "$p"
done
cp "$stubs"/* "$toolbox"/
rm -f "$toolbox/python3" "$toolbox/python3.exe"

# run NAME [VAR=val ...] -- [args]: stdout -> $tmp/NAME.out, stderr -> $tmp/NAME.err
run() {
  local name=$1; shift; local envs=()
  while [ "$1" != -- ]; do envs+=("$1"); shift; done; shift
  env -u CHECK_ENV_PIN ANDROID_HOME="$tmp/sdk" "${envs[@]}" PATH="$stubs:$PATH" \
    "$BASH" "$script" "$@" "$proj" > "$tmp/$name.out" 2> "$tmp/$name.err"
}
row() { awk -v t="$2" '$2 == t' "$tmp/$1.out"; }          # row CASE TOOL
status() { row "$1" "$2" | awk '{print $1}'; }
note() { row "$1" "$2" | cut -c60-; }                       # NOTE column starts at col 60

fails=0
check() { local d=$1; shift; if "$@"; then echo "PASS: $d"; else echo "FAIL: $d"; fails=$((fails + 1)); fi; }

run base --
check "(a) unpinned one-behind gh is UPDATE" [ "$(status base gh)" = UPDATE ]
check "(a) unpinned gradle-wrapper is UPDATE" [ "$(status base gradle-wrapper)" = UPDATE ]

run pin CHECK_ENV_PIN=gh --
check "(b) pinned gh is OK" [ "$(status pin gh)" = OK ]
check "(b) note is 'pinned: newer 2.103.0 available'" [ "$(note pin gh)" = "pinned: newer 2.103.0 available" ]
check "(b) other rows unchanged by pinning gh" \
  [ "$(grep -v ' gh ' "$tmp/base.out")" = "$(grep -v ' gh ' "$tmp/pin.out")" ]

# Space before GH: fails if whitespace is not stripped (ktlint is up to date, so only gh is affected).
run spaces "CHECK_ENV_PIN=ktlint, GH" --
check "(c) 'ktlint, GH' pins gh case-insensitively with spaces" [ "$(status spaces gh)" = OK ]
check "(c) note names the newer version" [ "$(note spaces gh)" = "pinned: newer 2.103.0 available" ]
check "(c) ktlint (up to date) still OK with empty note" [ "$(status spaces ktlint)/$(note spaces ktlint)" = "OK/" ]

run old STUB_JAVA=11.0.2 CHECK_ENV_PIN=jdk --
run oldbase STUB_JAVA=11.0.2 --
check "(d) pinned OLD jdk is still OLD" [ "$(status old jdk)" = OLD ]
check "(d) pinned OLD row identical to unpinned" [ "$(row old jdk)" = "$(row oldbase jdk)" ]

env -u CHECK_ENV_PIN ANDROID_HOME="$tmp/sdk" CHECK_ENV_PIN=python3 PATH="$toolbox" \
  "$BASH" "$script" "$proj" > "$tmp/miss.out" 2> "$tmp/miss.err"
check "(d) pinned MISSING python3 is still MISSING" [ "$(status miss python3)" = MISSING ]
check "(d) toolbox run still finds other tools (git OK)" [ "$(status miss git)" = OK ]

run unknown "CHECK_ENV_PIN=nosuchtool,, ,x" --
check "(e) unknown name: output identical to unpinned" cmp -s "$tmp/base.out" "$tmp/unknown.out"
check "(e) unknown name: nothing on stderr" [ ! -s "$tmp/unknown.err" ]

run off CHECK_ENV_PIN=gh -- --offline
run offbase -- --offline
check "(f) --offline + pin: gh is OK" [ "$(status off gh)" = OK ]
check "(f) --offline + pin: note is 'pinned'" [ "$(note off gh)" = pinned ]
check "(f) --offline unpinned gh has empty note" [ "$(status offbase gh)/$(note offbase gh)" = "OK/" ]
check "(f) --offline: unpinned rows unchanged" \
  [ "$(grep -v ' gh ' "$tmp/offbase.out")" = "$(grep -v ' gh ' "$tmp/off.out")" ]

run gw CHECK_ENV_PIN=gradle-wrapper --
check "(h) pinned gradle-wrapper is OK" [ "$(status gw gradle-wrapper)" = OK ]
check "(h) note is 'pinned: newer 9.0 available'" [ "$(note gw gradle-wrapper)" = "pinned: newer 9.0 available" ]

run empty CHECK_ENV_PIN= --
run other CHECK_ENV_PIN=nosuchtool --
check "(g) empty CHECK_ENV_PIN identical to unset" cmp -s "$tmp/base.out" "$tmp/empty.out"
check "(g) unrelated pin identical to unset" cmp -s "$tmp/base.out" "$tmp/other.out"

[ "$fails" = 0 ] && echo "All checks passed" || echo "$fails check(s) failed"
[ "$fails" = 0 ]
