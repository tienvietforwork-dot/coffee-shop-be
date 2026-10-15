#!/usr/bin/env bash
# Starts the backend apps locally, each as its own JVM, so one crashing never takes the others down.
# Config comes from the shared .env next to this script. Logs: logs/<app>.log
#   ./run-all.sh                 -> core crm stats promotions
#   ./run-all.sh core crm        -> only those
# Ctrl+C stops all of them.
cd "$(dirname "$0")" || exit 1

[ -f .env ] || { echo "Missing .env - copy .env.example to .env and fill in the DB / JWT_SECRET first." >&2; exit 1; }

apps=("$@")
[ ${#apps[@]} -eq 0 ] && apps=(core crm stats promotions)
mkdir -p logs
pids=()

for app in "${apps[@]}"; do
  [ -d "app-$app" ] || { echo "Unknown app '$app'" >&2; continue; }
  (cd "app-$app" && exec ./mvnw spring-boot:run) > "logs/$app.log" 2>&1 &
  pids+=($!)
  echo "started app-$app (log: logs/$app.log)"
done

trap 'kill "${pids[@]}" 2>/dev/null' INT TERM
wait
