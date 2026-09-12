# Will work only if script is run from project main directory with:
# ./config/k8s/_deployment-scripts/helm/linux/helm-app-upgrade.sh
#
# Pass `ENV=prod` (default) to layer the production overlay on top of each chart
# that ships one, matching helm-app-deploy.sh.

ENV="${ENV:-prod}"

# sealed secrets
helm upgrade sealed-secrets-controller ./config/k8s/helm/api-gateway/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller

# identity provider
helm upgrade keycloak ./config/k8s/helm/infra/keycloak/ \
  -f "./config/k8s/helm/infra/keycloak/values-${ENV}.yaml"

# api gateway
helm upgrade oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/ \
  -f "./config/k8s/helm/api-gateway/oauth2-proxy/values-${ENV}.yaml"

# independent services
helm upgrade kafka-service ./config/k8s/helm/kafka/ \
  -f "./config/k8s/helm/kafka/values-${ENV}.yaml"

# db
helm upgrade database-persistent-volume-claim ./config/k8s/helm/db/database-persistent-volume-claim/
helm upgrade postgres ./config/k8s/helm/db/postgres/
kubectl wait --namespace default --for=condition=ready --timeout=180s pod -l component=postgres

# object storage
helm upgrade floci ./config/k8s/helm/infra/floci/ \
  -f "./config/k8s/helm/infra/floci/values-${ENV}.yaml"

# services - each chart's defaults plus the per-env overlay
for svc in sky-booking sky-message sky-notify sky-offer; do
  helm upgrade "$svc" "./config/k8s/helm/service/$svc" \
    -f "./config/k8s/helm/service/$svc/values-${ENV}.yaml"
done
