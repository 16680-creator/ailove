#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAR_PATH="${JAR_PATH:-${APP_HOME}/backend/target/ai-love-daily-1.0.0.jar}"
LOG_DIR="${LOG_DIR:-${APP_HOME}/.run-logs}"
PID_FILE="${LOG_DIR}/backend.pid"
ENV_FILE="${SCRIPT_DIR}/.env"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

SPRING_PROFILE="${SPRING_PROFILE:-prod}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
FILE_UPLOAD_PATH="${FILE_UPLOAD_PATH:-${APP_HOME}/data/uploads}"

mkdir -p "${LOG_DIR}" "${FILE_UPLOAD_PATH}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Jar not found: ${JAR_PATH}"
  exit 1
fi

if [[ -f "${PID_FILE}" ]]; then
  OLD_PID="$(cat "${PID_FILE}")"
  if [[ -n "${OLD_PID}" ]] && kill -0 "${OLD_PID}" 2>/dev/null; then
    echo "Backend is already running. PID=${OLD_PID}"
    exit 1
  fi
fi

cd "${APP_HOME}"

nohup java ${JAVA_OPTS} -jar "${JAR_PATH}" \
  --spring.profiles.active="${SPRING_PROFILE}" \
  > "${LOG_DIR}/backend.out.log" \
  2> "${LOG_DIR}/backend.err.log" &

echo $! > "${PID_FILE}"
echo "Backend started. PID=$(cat "${PID_FILE}")"
echo "Logs: ${LOG_DIR}/backend.out.log"
