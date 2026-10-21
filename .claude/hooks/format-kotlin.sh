#!/usr/bin/env bash
# PostToolUse hook: format a single edited .kt/.kts file with ktlint or ktfmt if one is on PATH.
# Never blocks: missing formatter or formatter failure exits 0 and is reported on stderr only.

input="$(cat)"
file="$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n1)"
file="${file//\\\\//}"

case "$file" in
  *.kt|*.kts) ;;
  *) exit 0 ;;
esac
[ -f "$file" ] || exit 0

if command -v ktlint >/dev/null 2>&1; then
  ktlint -F --relative "$file" >/dev/null 2>&1 || echo "ktlint reported issues in $file (run: ktlint $file)" >&2
elif command -v ktfmt >/dev/null 2>&1; then
  ktfmt --kotlinlang-style "$file" >/dev/null 2>&1 || echo "ktfmt failed on $file" >&2
fi
exit 0
