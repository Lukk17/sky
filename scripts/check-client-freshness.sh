#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CONTRACT="${REPO_ROOT}/docs/api/contract/openapi.yaml"
CLIENT_DIR="${REPO_ROOT}/apps/frontend/sky-view/src/app/api-generated"

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

npx --yes @openapitools/openapi-generator-cli@latest generate \
  -i "${CONTRACT}" \
  -g typescript-angular \
  -o "$TMP_DIR/api-generated" \
  --skip-validate-spec \
  --additional-properties=ngVersion=22.1.0,configurationPrefix=SkyApi,providedInRoot=false

diff -r "${CLIENT_DIR}/api" "$TMP_DIR/api-generated/api"
diff -r "${CLIENT_DIR}/model" "$TMP_DIR/api-generated/model"
