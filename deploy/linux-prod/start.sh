#!/usr/bin/env bash

set -euo pipefail

APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${APP_HOME}/.env"
JAR_PATH="${APP_HOME}/app.jar"
LOG_DIR="${APP_HOME}/logs"
RUN_DIR="${APP_HOME}/run"
PID_FILE="${RUN_DIR}/app.pid"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

SPRING_PROFILE="${SPRING_PROFILE:-prod}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
SERVER_PORT="${SERVER_PORT:-8080}"
FILE_UPLOAD_PATH="${FILE_UPLOAD_PATH:-${APP_HOME}/uploads}"

mkdir -p "${LOG_DIR}" "${RUN_DIR}" "${FILE_UPLOAD_PATH}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "Jar not found: ${JAR_PATH}"
  exit 1
fi

if [[ -f "${PID_FILE}" ]]; then
  OLD_PID="$(cat "${PID_FILE}")"
  if [[ -n "${OLD_PID}" ]] && kill -0 "${OLD_PID}" 2>/dev/null; then
    echo "Application is already running. PID=${OLD_PID}"
    exit 1
  fi
fi

cd "${APP_HOME}"

nohup java ${JAVA_OPTS} -jar "${JAR_PATH}" \
  --spring.profiles.active="${SPRING_PROFILE}" \
  --server.port="${SERVER_PORT}" \
  > "${LOG_DIR}/backend.out.log" \
  2> "${LOG_DIR}/backend.err.log" &

echo $! > "${PID_FILE}"
echo "Application started. PID=$(cat "${PID_FILE}")"
echo "Profile: ${SPRING_PROFILE}"
echo "Port: ${SERVER_PORT}"
echo "Logs: ${LOG_DIR}/backend.out.log"
