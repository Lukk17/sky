#!/bin/bash
# Verifies every hostname the local stacks depend on resolves to loopback,
# plus the gateway edge answers on its host ports.
#
# Names checked (all must resolve to loopback, 127.0.0.0/8 or ::1):
#   localhost                 - gateway, frontend, health probes
#   s3.localhost              - object-store presigned URLs via the gateway (/s3)
#   sky-verify-<pid>.localhost - canary proving wildcard *.localhost resolves,
#                               not just one hardcoded hosts entry
#   keycloak.test             - host-run services default to
#                               OAUTH2_ISSUER_URI=https://keycloak.test:9443/realms/sky
#
# Ports checked (TCP connect from the host):
#   127.0.0.1:5777 - the single edge for compose and k3d (API, Keycloak /auth, store /s3)
#   127.0.0.1:80   - k3d only, for portless Keycloak browser URLs; info only,
#                    compose does not publish it
#
# Run from the repository root or from anywhere:
#   bash scripts/verify-host-dns.sh
#   bash scripts/verify-host-dns.sh --require-gateway
#
# Default mode checks DNS strictly and reports gateway ports without failing,
# so it works as the first step before the stack is up. Pass --require-gateway
# after bring-up (or in CI before the Bruno gate) to also fail when 5777
# does not answer. Port 80 never affects the exit code.
#
# Exit 0 when every required check passes. Exit 1 naming the failing host otherwise.
set -euo pipefail

GATEWAY_HOST="127.0.0.1"
GATEWAY_PORT="5777"
REQUIRE_GATEWAY=0

usage() {
  printf 'Usage: bash scripts/verify-host-dns.sh [--require-gateway] [--gateway-host HOST] [--gateway-port PORT]\n'
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --require-gateway)
      REQUIRE_GATEWAY=1
      shift
      ;;
    --gateway-host)
      GATEWAY_HOST="${2:?--gateway-host needs a value}"
      shift 2
      ;;
    --gateway-port)
      GATEWAY_PORT="${2:?--gateway-port needs a value}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      printf 'ERROR: unknown argument "%s".\n' "$1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

failed=0

is_loopback() {
  case "$1" in
    127.*|::1) return 0 ;;
    *) return 1 ;;
  esac
}

check_host() {
  local name="$1"
  local hint="${2:-}"
  local output
  local ips
  output="$(getent hosts "${name}" || true)"
  if [ -z "${output}" ]; then
    printf 'FAIL: %s does not resolve (no getent hosts answer).\n' "${name}" >&2
    [ -n "${hint}" ] && printf '      Fix: %s\n' "${hint}" >&2
    failed=1
    return 1
  fi
  ips="$(printf '%s\n' "${output}" | awk '{print $1}' | sort -u | tr '\n' ' ')"
  while read -r ip _rest; do
    [ -z "${ip}" ] && continue
    if is_loopback "${ip}"; then
      printf 'OK: %s resolves to loopback (%s).\n' "${name}" "${ips% }"
      return 0
    fi
  done <<< "${output}"
  printf 'FAIL: %s resolves to %s, not loopback.\n' "${name}" "${ips% }" >&2
  [ -n "${hint}" ] && printf '      Fix: %s\n' "${hint}" >&2
  failed=1
  return 1
}

check_port() {
  local host="$1"
  local port="$2"
  local label="$3"
  local required="$4"
  if (exec 3<>"/dev/tcp/${host}/${port}") 2>/dev/null; then
    exec 3<&- 3>&- || true
    printf 'OK: %s answers on %s:%s.\n' "${label}" "${host}" "${port}"
    return 0
  fi
  if [ "${required}" -eq 1 ]; then
    printf 'FAIL: %s does not answer on %s:%s (connection refused or timed out).\n' \
      "${label}" "${host}" "${port}" >&2
    failed=1
    return 1
  fi
  printf 'INFO: %s does not answer on %s:%s yet (stack not up, or k3d-only port).\n' \
    "${label}" "${host}" "${port}"
  return 0
}

check_host "localhost" "Loopback must resolve; check /etc/hosts carries '127.0.0.1 localhost'." || true
check_host "s3.localhost" "Wildcard *.localhost must resolve to loopback; on systemd this is automatic, otherwise add '127.0.0.1 s3.localhost' to /etc/hosts." || true
check_host "sky-verify-$$.localhost" "Wildcard *.localhost does not resolve; a single s3.localhost entry is not enough, the resolver must map every name under .localhost to 127.0.0.1." || true
check_host "keycloak.test" 'Add it: echo "127.0.0.1 keycloak.test" | sudo tee -a /etc/hosts' || true

check_port "${GATEWAY_HOST}" "${GATEWAY_PORT}" "Gateway edge" "${REQUIRE_GATEWAY}" || true
check_port "${GATEWAY_HOST}" "80" "Gateway portless HTTP (k3d only)" 0 || true

if [ "${failed}" -ne 0 ]; then
  printf '\nHost and DNS verification failed. Fix the FAIL line above, then rerun bash scripts/verify-host-dns.sh.\n' >&2
  exit 1
fi

printf '\nAll required host and DNS checks passed.\n'
