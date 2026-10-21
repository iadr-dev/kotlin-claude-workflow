#!/usr/bin/env bash
# Read-only environment check for this Kotlin workflow. Works on Windows (Git Bash), macOS, Linux.
# Prints one line per tool: STATUS  name  installed  latest  note
# STATUS: OK | UPDATE (older than latest) | MISSING (required) | OPTIONAL (not installed) | OLD (below minimum)
# Usage: bash check-env.sh [--offline] [project-dir]

offline=0
[ "$1" = "--offline" ] && { offline=1; shift; }
project="${1:-.}"

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) os=windows ;;
  Darwin) os=macos ;;
  *) os=linux ;;
esac

fetch() { [ $offline = 1 ] && return 1; curl -fsSL --max-time 10 "$1" 2>/dev/null; }
# Latest release tag via the redirect of /releases/latest (no API rate limit).
gh_latest() {
  [ $offline = 1 ] && return 0
  curl -fsSLI -o /dev/null -w '%{url_effective}' --max-time 10 "https://github.com/$1/releases/latest" 2>/dev/null \
    | sed -n 's#.*/tag/##p' | sed 's#^.*/##; s#^v##'
}
first_ver() { grep -oE '[0-9]+(\.[0-9]+)+' | head -n1; }
# ver_lt A B -> true if A < B
ver_lt() { [ "$1" != "$2" ] && [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | head -n1)" = "$1" ]; }

report() { # status name installed latest note
  printf '%-9s %-16s %-16s %-14s %s\n' "$1" "$2" "${3:--}" "${4:--}" "$5"
}
judge() { # name installed latest required(1/0) note [minimum]
  local name=$1 inst=$2 latest=$3 req=$4 note=$5 min=$6
  if [ -z "$inst" ]; then
    [ "$req" = 1 ] && report MISSING "$name" "" "$latest" "$note" || report OPTIONAL "$name" "" "$latest" "$note"
  elif [ -n "$min" ] && ver_lt "$inst" "$min"; then
    report OLD "$name" "$inst" "$latest" "needs >= $min. $note"
  elif [ -n "$latest" ] && ver_lt "$inst" "$latest"; then
    report UPDATE "$name" "$inst" "$latest" "$note"
  else
    report OK "$name" "$inst" "$latest" ""
  fi
}

echo "OS: $os    project: $project    $( [ $offline = 1 ] && echo '(offline: latest versions skipped)')"
printf '%-9s %-16s %-16s %-14s %s\n' STATUS TOOL INSTALLED LATEST NOTE

# --- core ---
judge git "$(git --version 2>/dev/null | first_ver)" "" 1 "version control"

jdk_inst="$(java -version 2>&1 | first_ver)"
jdk_lts="$(fetch https://api.adoptium.net/v3/info/available_releases | grep -o '"most_recent_lts"[^0-9]*[0-9]*' | grep -oE '[0-9]+$')"
jdk_major="${jdk_inst%%.*}"
judge jdk "$jdk_major" "$jdk_lts" 1 "LTS JDK for Gradle/AGP (Temurin recommended)" 17

py=""; command -v python3 >/dev/null 2>&1 && py="$(python3 --version 2>&1 | first_ver)"
judge python3 "$py" "" 1 "required by gradle-run skill (command must be named python3)"

judge gh "$(gh --version 2>/dev/null | first_ver)" "$(gh_latest cli/cli)" 1 "GitHub workflows (to-plan, deliver-spec, shepherd); run: gh auth login"

# --- android ---
and_inst="$(android --version 2>/dev/null | first_ver)"
judge android-cli "$and_inst" "" 1 "Android CLI; no read-only latest check exists, so run: android update (idempotent)"

# The Windows launcher is versioned separately and winget can report an available upgrade.
if [ "$os" = windows ] && [ $offline = 0 ] && command -v winget >/dev/null 2>&1; then
  wg="$(winget list --id Google.AndroidCLI --upgrade-available --accept-source-agreements 2>/dev/null | grep 'Google.AndroidCLI')"
  if [ -n "$wg" ]; then
    set -- $(printf '%s' "$wg" | grep -oE '[0-9]+(\.[0-9]+)+')
    report UPDATE android-launcher "$1" "$2" "run: winget upgrade --id Google.AndroidCLI"
  fi
fi

# Official Android skills: report coverage; `update --all` keeps installed ones current.
if [ -n "$and_inst" ] && [ $offline = 0 ]; then
  sk="$(android skills list 2>/dev/null | sed 's/\x1b\[[0-9;]*m//g' | awk '
    BEGIN { RS = ""; FS = "\n" }
    { if ($1 ~ /^Downloading/) next; total++
      if ($0 ~ /Installed for/) inst++; else missing = missing (missing ? ", " : "") $1 }
    END { printf "%d %d %s", inst + 0, total + 0, missing }')"
  s_inst="${sk%% *}"; rest="${sk#* }"; s_total="${rest%% *}"; s_missing="${rest#* }"
  if [ "${s_total:-0}" -eq 0 ]; then report OPTIONAL android-skills "" "" "could not list skills (network?); try: android skills list"
  elif [ "$s_inst" -eq 0 ]; then report MISSING android-skills "0/$s_total" "-" "run: android skills add <id> (start with android-cli)"
  else
    note="keep current with: android skills update --all"
    [ "$s_inst" -lt "$s_total" ] && note="$note; not installed: $s_missing"
    report OK android-skills "$s_inst/$s_total installed" "-" "$note"
  fi
fi

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -z "$sdk" ] && [ -n "$and_inst" ] && sdk="$(android info 2>/dev/null | sed -n 's/^sdk:[[:space:]]*//p')"
if [ -n "$sdk" ] && [ -d "$sdk" ]; then report OK android-sdk "found" "-" "$sdk"
else report MISSING android-sdk "" "" "set ANDROID_HOME or run: android sdk install platform-tools"; fi

judge adb "$(adb version 2>/dev/null | sed -n 's/.*Version //p' | first_ver)" "" 1 "platform-tools; update with: android sdk update platform-tools"

# --- code intelligence / formatting ---
klsp=""
for c in kotlin-lsp kotlin-lsp.cmd intellij-server; do
  command -v "$c" >/dev/null 2>&1 && { klsp="$(timeout 30 "$c" --version 2>/dev/null | first_ver)"; [ -n "$klsp" ] && break; }
done
judge kotlin-lsp "$klsp" "$(gh_latest Kotlin/kotlin-lsp | first_ver)" 0 "for kotlin-lsp plugin; must be on PATH as 'kotlin-lsp'"

judge ktlint "$(ktlint --version 2>/dev/null | first_ver)" "$(gh_latest ktlint/ktlint)" 0 "format hook (or ktfmt)"
command -v ktfmt >/dev/null 2>&1 && judge ktfmt "$(ktfmt --version 2>/dev/null | first_ver)" "$(gh_latest facebook/ktfmt)" 0 ""

# --- Claude Code plugins declared in .claude/settings.json ---
settings="$project/.claude/settings.json"
if [ -f "$settings" ] && command -v claude >/dev/null 2>&1; then
  echo
  echo "Claude Code plugins (declared in .claude/settings.json):"
  installed="$(claude plugin list 2>/dev/null)"
  sed -n '/"enabledPlugins"/,/}/p' "$settings" | grep -oE '"[^"]+@[^"]+"[[:space:]]*:[[:space:]]*true' | grep -oE '^"[^"]+"' | tr -d '"' |
  while read -r p; do
    if printf '%s\n' "$installed" | grep -qF "$p"; then report OK "$p" "installed" "-" ""
    else report MISSING "$p" "" "-" "run: claude plugin install $p --scope project"; fi
  done
fi

# --- project toolchain (only inside a Gradle project) ---
wp="$project/gradle/wrapper/gradle-wrapper.properties"
if [ -f "$wp" ]; then
  echo
  echo "Project toolchain:"
  gw="$(sed -n 's/.*gradle-\([0-9.]*\)-.*/\1/p' "$wp")"
  judge gradle-wrapper "$gw" "$(fetch https://services.gradle.org/versions/current | grep -o '"version" *: *"[^"]*"' | first_ver)" 1 \
    "update: ./gradlew wrapper --gradle-version <v>"
  toml="$project/gradle/libs.versions.toml"
  if [ -f "$toml" ]; then
    kv="$(grep -E '^[[:space:]]*kotlin[[:space:]]*=' "$toml" | first_ver)"
    [ -n "$kv" ] && judge kotlin "$kv" "$(gh_latest JetBrains/kotlin)" 1 "libs.versions.toml [versions] kotlin"
    av="$(grep -E '^[[:space:]]*(agp|androidGradlePlugin|android-gradle-plugin)[[:space:]]*=' "$toml" | first_ver)"
    if [ -n "$av" ]; then
      agp_latest="$(fetch https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml \
        | grep -oE '<version>[0-9]+\.[0-9]+\.[0-9]+</version>' | sed 's/<[^>]*>//g' | sort -V | tail -n1)"
      judge agp "$av" "$agp_latest" 1 "major upgrades: use agp-9-upgrade skill"
    fi
  fi
fi
exit 0
