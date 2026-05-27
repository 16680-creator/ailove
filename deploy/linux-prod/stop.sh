#!/usr/bin/env bash

set -euo pipefail

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="${APP_HOME}/run/app.pid"

if [[ ! -f "${PID_FILE}" ]]; then
  echo "PID file not found: ${PID_FILE}"
  exit 1
fi

PID="$(cat "${PID_FILE}")"

if [[ -z "${PID}" ]] || ! kill -0 "${PID}" 2>/dev/null; then
  echo "Process is not running."
  rm -f "${PID_FILE}"
  exit 1
fi

kill "${PID}"
rm -f "${PID_FILE}"
echo "Application stopped. PID=${PID}"
