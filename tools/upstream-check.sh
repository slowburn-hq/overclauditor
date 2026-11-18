#!/bin/bash
# Once a day: fetch Clauditor (remote `upstream`) and list commits we haven't taken.
# Silent when already checked today or when there's nothing new. --force skips the daily gate.
set -euo pipefail
cd "$(dirname "$0")/.."
stamp="$HOME/.overclauditor/upstream-checked"
today=$(date +%F)
if [ "${1:-}" != "--force" ] && [ "$(cat "$stamp" 2>/dev/null)" = "$today" ]; then exit 0; fi
git fetch -q upstream
mkdir -p "$(dirname "$stamp")" && echo "$today" > "$stamp"
# --cherry-pick drops upstream commits whose patch we already carry under another SHA
# (upstream rewrote history once, so main..upstream/main listed 18 commits we already had).
new=$(git log --oneline --no-merges --cherry-pick --right-only main...upstream/main)
[ -z "$new" ] && exit 0
echo "Clauditor has $(echo "$new" | wc -l | tr -d ' ') new commit(s) since our last merge:"
echo "$new"
echo "-> follow docs/upstream-updates.md"
