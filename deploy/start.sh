#!/bin/sh
set -eu

case "$PORT" in
  ''|*[!0-9]*) echo "PORT must be a numeric TCP port" >&2; exit 2 ;;
esac
if [ "$PORT" -lt 1 ] || [ "$PORT" -gt 65535 ]; then
  echo "PORT must be between 1 and 65535" >&2
  exit 2
fi

if [ "$(id -u)" -eq 0 ]; then
  install -d -m 0700 -o ledger -g ledger "$LEDGER_BROWSER_DATA"
  exec gosu ledger "$0" "$@"
fi

exec /bin/bash /app/bin/start.sh "-Dhttp.port=$PORT" "$@"
