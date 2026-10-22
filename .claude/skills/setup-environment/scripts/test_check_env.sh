#!/usr/bin/env bash
# Self-contained test for check-env.sh (CHECK_ENV_PIN). Offline-safe: stand-in executables are placed
# first on PATH, so no real tool is downgraded and nothing touches the network.
# Usage: bash test_check_env.sh
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
script="$here/check-env.sh"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
stubs="$tmp/stubs"; nopy="$tmp/stubs-nopy"; proj="$tmp/project"
mkdir -p "$stubs" "$proj" "$tmp/sdk"

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
# curl: only the releases/latest redirect lookup answers; everything else fails (latest unknown).
stub "$stubs" curl 'for a in "$@"; do case "$a" in
  */cli/cli/releases/latest) printf "https://github.com/cli/cli/releases/tag/v2.103.0"; exit 0 ;;
  */ktlint/ktlint/releases/latest) printf "https://github.com/ktlint/ktlint/releases/tag/1.3.0"; exit 0 ;;
esac; done; exit 22'
cp "$stubs"/* "$nopy"/ 2>/dev/null || { mkdir -p "$nopy"; cp "$stubs"/* "$nopy"/; }
rm -f "$nopy/python3"

# PATH without any directory that provides python3 (used to simulate a missing tool).
nopy_path=""
IFS=: read -ra dirs <<< "$PATH"
for d in "${dirs[@]}"; do
  { [ -x "$d/python3" ] || [ -x "$d/python3.exe" ]; } && continue
  nopy_path="$nopy_path:$d"
done

# run NAME [env VAR=val ...] -- [args]: stdout -> $tmp/NAME.out, stderr -> $tmp/NAME.err
run() {
  local name=$1; shift; local envs=()
  while [ "$1" != -- ]; do envs+=("$1"); shift; done; shift
  env -u CHECK_ENV_PIN ANDROID_HOME="$tmp/sdk" "${envs[@]}" PATH="$stubs:$PATH" \
    bash "$script" "$@" "$proj" > "$tmp/$name.out" 2> "$tmp/$name.err"
}
row() { awk -v t="$2" '$2 == t' "$tmp/$1.out"; }          # row CASE TOOL
status() { row "$1" "$2" | awk '{print $1}'; }
note() { row "$1" "$2" | cut -c60-; }  # NOTE column starts at col 60

fails=0
check() { # description, condition result (0 = pass)
  if [ "$2" = 0 ]; then echo "PASS: $1"; else echo "FAIL: $1"; fails=$((fails + 1)); fi
}
eq() { [ "$1" = "$2" ]; }

run base -- 
check "(a) unpinned one-behind gh is UPDATE" "$(eq "$(status base gh)" UPDATE; echo $?)"

run pin CHECK_ENV_PIN=gh --
check "(b) pinned gh is OK" "$(eq "$(status pin gh)" OK; echo $?)"
check "(b) note is 'pinned: newer 2.103.0 available'" "$(eq "$(note pin gh)" "pinned: newer 2.103.0 available"; echo $?)"
check "(b) other rows unchanged by pinning gh" \
  "$(eq "$(grep -v ' gh ' "$tmp/base.out")" "$(grep -v ' gh ' "$tmp/pin.out")"; echo $?)"

run spaces "CHECK_ENV_PIN=GH, ktlint" --
check "(c) 'GH, ktlint' pins gh case-insensitively" "$(eq "$(status spaces gh)" OK; echo $?)"
check "(c) note names the newer version" "$(eq "$(note spaces gh)" "pinned: newer 2.103.0 available"; echo $?)"
check "(c) ktlint (up to date) still OK with empty note" \
  "$(eq "$(status spaces ktlint)/$(note spaces ktlint)" "OK/"; echo $?)"

run old STUB_JAVA=11.0.2 CHECK_ENV_PIN=jdk --
run oldbase STUB_JAVA=11.0.2 --
check "(d) pinned OLD jdk is still OLD" "$(eq "$(status old jdk)" OLD; echo $?)"
check "(d) pinned OLD row identical to unpinned" "$(eq "$(row old jdk)" "$(row oldbase jdk)"; echo $?)"

env -u CHECK_ENV_PIN ANDROID_HOME="$tmp/sdk" CHECK_ENV_PIN=python3 PATH="$nopy:$nopy_path" \
  bash "$script" "$proj" > "$tmp/miss.out" 2> "$tmp/miss.err"
check "(d) pinned MISSING python3 is still MISSING" "$(eq "$(status miss python3)" MISSING; echo $?)"

run unknown CHECK_ENV_PIN=nosuchtool,,\ ,x --
check "(e) unknown name: output identical to unpinned" "$(cmp -s "$tmp/base.out" "$tmp/unknown.out"; echo $?)"
check "(e) unknown name: nothing on stderr" "$(eq "$(cat "$tmp/unknown.err")" ""; echo $?)"

run off CHECK_ENV_PIN=gh -- --offline
run offbase -- --offline
check "(f) --offline + pin: gh is OK" "$(eq "$(status off gh)" OK; echo $?)"
check "(f) --offline + pin: note is 'pinned'" "$(eq "$(note off gh)" pinned; echo $?)"
check "(f) --offline unpinned gh has empty note" "$(eq "$(status offbase gh)/$(note offbase gh)" "OK/"; echo $?)"
check "(f) --offline: unpinned rows unchanged" \
  "$(eq "$(grep -v ' gh ' "$tmp/offbase.out")" "$(grep -v ' gh ' "$tmp/off.out")"; echo $?)"

run empty CHECK_ENV_PIN= --
run other CHECK_ENV_PIN=nosuchtool --
check "(g) empty CHECK_ENV_PIN identical to unset" "$(cmp -s "$tmp/base.out" "$tmp/empty.out"; echo $?)"
check "(g) unrelated pin identical to unset" "$(cmp -s "$tmp/base.out" "$tmp/other.out"; echo $?)"
git -C "$here" show 801fca0:.claude/skills/setup-environment/scripts/check-env.sh > "$tmp/base-check-env.sh" 2>/dev/null
if [ -s "$tmp/base-check-env.sh" ]; then
  env -u CHECK_ENV_PIN ANDROID_HOME="$tmp/sdk" PATH="$stubs:$PATH" bash "$tmp/base-check-env.sh" "$proj" > "$tmp/orig.out" 2>&1
  check "(g) unset output byte-identical to base commit 801fca0" "$(cmp -s "$tmp/orig.out" "$tmp/base.out"; echo $?)"
  env -u CHECK_ENV_PIN ANDROID_HOME="$tmp/sdk" PATH="$stubs:$PATH" bash "$tmp/base-check-env.sh" --offline "$proj" > "$tmp/orig-off.out" 2>&1
  check "(g) --offline output byte-identical to base commit" "$(cmp -s "$tmp/orig-off.out" "$tmp/offbase.out"; echo $?)"
else
  check "(g) base commit 801fca0 readable via git show" 1
fi

[ "$fails" = 0 ] && echo "All checks passed" || echo "$fails check(s) failed"
[ "$fails" = 0 ]
