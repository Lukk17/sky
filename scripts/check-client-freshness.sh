#!/bin/bash
set -euo pipefail

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

npx --yes @openapitools/openapi-generator-cli@latest generate \
  -i /home/lukk/Projects/sky/contracts/sky-api/openapi.yaml \
  -g typescript-angular \
  -o "$TMP_DIR/api-generated" \
  --skip-validate-spec \
  --additional-properties=ngVersion=22.1.0,useCustomConfigurationClass=true,configurationClass=SkyApiConfiguration,providedInRoot=false

diff -r /home/lukk/Projects/sky/apps/frontend/sky-view/src/app/api-generated/api "$TMP_DIR/api-generated/api" >/dev/null
diff -r /home/lukk/Projects/sky/apps/frontend/sky-view/src/app/api-generated/model "$TMP_DIR/api-generated/model" >/dev/null
