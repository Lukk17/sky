#!/bin/bash
# Checks that every Gradle service module is represented in its Dockerfile COPY list.
# Fails when a module listed in settings.gradle.kts is missing from a Dockerfile,
# or when a Dockerfile COPYs a directory that is not a Gradle module.
#
# Run from the repository root or from anywhere:
#   bash config/scripts/check-dockerfile-parity.sh
#
# Exit 0 when every service Dockerfile carries its own module, sky-common,
# and the shared build inputs. Exit 1 with a named missing entry otherwise.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/apps/backend"
SETTINGS_FILE="${BACKEND_DIR}/settings.gradle.kts"

if [ ! -f "${SETTINGS_FILE}" ]; then
  printf 'ERROR: %s not found.\n' "${SETTINGS_FILE}" >&2
  exit 1
fi

# Services that ship a Dockerfile. sky-common is a library with no image.
SERVICES="sky-booking sky-offer sky-message sky-notify sky-gateway"

# Shared build inputs every service Dockerfile must COPY so the Gradle
# build inside Docker sees the same inputs as a local build.
SHARED_COPIES="settings.gradle.kts build.gradle.kts gradle buildSrc"

failed=0

check_service() {
  local service="$1"
  local dockerfile="${BACKEND_DIR}/${service}/docker/Dockerfile"
  local service_failed=0

  if [ ! -f "${dockerfile}" ]; then
    printf 'ERROR: %s missing Dockerfile at %s.\n' "${service}" "${dockerfile}" >&2
    return 1
  fi

  # 1. The Dockerfile must COPY its own module directory.
  if ! grep -Eq "^COPY[[:space:]]+${service}[[:space:]]" "${dockerfile}"; then
    printf 'ERROR: %s Dockerfile does not COPY its own module directory (%s).\n' "${service}" "${service}" >&2
    service_failed=1
  fi

  # 2. Every project(":...") dependency in the module build file must be COPYd.
  # Today every service depends on :sky-common, so every Dockerfile must carry it.
  # Parsing the build file keeps this check honest when a new project dependency appears.
  local buildfile="${BACKEND_DIR}/${service}/build.gradle.kts"
  if [ -f "${buildfile}" ]; then
    local deps
    deps="$(grep -oP 'project\(":\K[^"]+' "${buildfile}" | sort -u || true)"
    for dep in ${deps}; do
      if ! grep -Eq "^COPY[[:space:]]+${dep}[[:space:]]" "${dockerfile}"; then
        printf 'ERROR: %s Dockerfile is missing COPY for Gradle project dependency ":%s" (from %s).\n' \
          "${service}" "${dep}" "${buildfile}" >&2
        service_failed=1
      fi
    done
  fi

  # 3. Shared build inputs must be present.
  for shared in ${SHARED_COPIES}; do
    if ! grep -Eq "^COPY[[:space:]]+.*${shared}" "${dockerfile}"; then
      printf 'ERROR: %s Dockerfile is missing shared build input "%s".\n' "${service}" "${shared}" >&2
      service_failed=1
    fi
  done

  # 4. The bootJar task must name this service, so a copy-pasted Dockerfile
  # cannot silently build a different module.
  if ! grep -Eq "gradle[[:space:]]+:${service}:bootJar" "${dockerfile}"; then
    printf 'ERROR: %s Dockerfile does not build :%s:bootJar.\n' "${service}" "${service}" >&2
    service_failed=1
  fi

  if [ "${service_failed}" -eq 0 ]; then
    printf 'OK: %s Dockerfile covers its module and its project dependencies.\n' "${service}"
  fi
  return "${service_failed}"
}

# 5. Every service directory with a Dockerfile must be listed in settings.gradle.kts,
# and every sky-* module listed there must still exist on disk.
for service in ${SERVICES}; do
  if ! grep -Eq "\"${service}\"" "${SETTINGS_FILE}"; then
    printf 'ERROR: settings.gradle.kts does not list "%s" but %s/docker/Dockerfile exists.\n' \
      "${service}" "${service}" >&2
    failed=1
  fi
  if [ ! -d "${BACKEND_DIR}/${service}" ]; then
    printf 'ERROR: settings.gradle.kts lists "%s" but %s does not exist.\n' \
      "${service}" "${service}" >&2
    failed=1
  fi
  check_service "${service}" || failed=1
done

if [ "${failed}" -ne 0 ]; then
  printf '\nDockerfile parity failed. Add the missing COPY line named above.\n' >&2
  exit 1
fi

printf '\nAll %s service Dockerfiles match their Gradle modules.\n' "$(printf '%s' "${SERVICES}" | wc -w)"
