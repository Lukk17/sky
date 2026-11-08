#!/usr/bin/env bash
# Runs the Bruno e2e collection against a freshly built local compose stack.
#
# Usage:
#   bash config/scripts/run-e2e.sh
#
# The runner resolves the repository root from this file's path, so it works
# from any directory. It stops the compose stack on exit and keeps named
# volumes. It never prunes images, build cache, or volumes.
#
# Exit codes:
#   0  the compose stack came up and the Bruno collection passed
#   1  the compose stack failed, a file was missing, or the health check timed out
#   2  docker, curl, or the Bruno CLI is missing
#   *  the Bruno CLI exit code
set -euo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
COMPOSE_DIR="${REPO_ROOT}/config/docker"
COMPOSE_FILE="${COMPOSE_DIR}/docker-compose.yaml"
BRUNO_DIR="${REPO_ROOT}/docs/api/request"
HEALTH_URL="http://localhost:5777/actuator/health"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-300}"
HEALTH_POLL_SECONDS="${HEALTH_POLL_SECONDS:-2}"

STACK_SERVICES=(
  postgres
  keycloak
  floci
  kafka
  sky-booking
  sky-offer
  sky-message
  sky-notify
  sky-gateway
)

compose() {
  docker compose --project-directory "${COMPOSE_DIR}" --file "${COMPOSE_FILE}" "$@"
}

cleanup() {
  local status
  status=$?

  trap - EXIT

  printf 'Stopping the compose stack...\n'
  if ! compose down --remove-orphans; then
    printf 'WARNING: docker compose down failed. Remove stale containers with:\n' >&2
    printf '  docker compose --project-directory "%s" --file "%s" down --remove-orphans\n' \
      "${COMPOSE_DIR}" "${COMPOSE_FILE}" >&2
  fi

  exit "${status}"
}

health_is_up() {
  curl --silent --fail --max-time 5 "${HEALTH_URL}" 2>/dev/null \
    | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'
}

wait_for_gateway() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))

  while ((SECONDS < deadline)); do
    if health_is_up; then
      printf 'Gateway reports UP at %s after %s seconds.\n' "${HEALTH_URL}" "${SECONDS}"
      return 0
    fi
    sleep "${HEALTH_POLL_SECONDS}"
  done

  printf 'ERROR: %s did not report UP within %s seconds.\n' \
    "${HEALTH_URL}" "${HEALTH_TIMEOUT_SECONDS}" >&2
  printf 'Inspect the gateway logs with:\n' >&2
  printf '  docker compose --project-directory "%s" --file "%s" logs sky-gateway\n' \
    "${COMPOSE_DIR}" "${COMPOSE_FILE}" >&2
  return 1
}

main() {
  printf 'Stopping any existing compose stack...\n'
  compose down --remove-orphans

  printf 'Building and starting the compose stack...\n'
  compose up -d --build "${STACK_SERVICES[@]}"

  printf 'Waiting for the gateway at %s...\n' "${HEALTH_URL}"
  wait_for_gateway

  printf 'Seeding the local dataset...\n'
  compose run --rm --no-deps seed

  printf 'Running the Bruno collection...\n'
  local bruno_status=0
  (cd "${BRUNO_DIR}" && bru run -r --env local) || bruno_status=$?
  return "${bruno_status}"
}

if ! command -v docker >/dev/null 2>&1; then
  printf 'ERROR: docker is required but was not found on PATH.\n' >&2
  exit 2
fi

if ! command -v curl >/dev/null 2>&1; then
  printf 'ERROR: curl is required but was not found on PATH.\n' >&2
  exit 2
fi

if ! command -v bru >/dev/null 2>&1; then
  printf 'ERROR: the Bruno CLI (bru) is required but was not found on PATH.\n' >&2
  printf 'Install it with: npm install --global @usebruno/cli\n' >&2
  exit 2
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  printf 'ERROR: compose file not found at %s.\n' "${COMPOSE_FILE}" >&2
  exit 1
fi

if [[ ! -d "${BRUNO_DIR}" ]]; then
  printf 'ERROR: Bruno collection directory not found at %s.\n' "${BRUNO_DIR}" >&2
  exit 1
fi

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

main "$@"
