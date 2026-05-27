#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${LOG_DIR:-${APP_HOME}/.run-logs}"
PID_FILE="${LOG_DIR}/backend.pid"

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
echo "Backend stopped. PID=${PID}"
