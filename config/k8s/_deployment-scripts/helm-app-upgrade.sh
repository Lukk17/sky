# Will work only if script is run from project main directory with:
# ./config/k8s/_deployment-scripts/helm-app-upgrade.sh

# sealed secrets
helm upgrade sealed-secrets-controller ./config/k8s/helm/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller

# api gateway
helm upgrade oauth2-proxy ./config/k8s/helm/oauth2-proxy/

# independent services
helm upgrade kafka ./config/k8s/helm/kafka/

# db
helm upgrade mysql ./config/k8s/helm/mysql/
kubectl wait --namespace default --for=condition=ready --timeout=120s pod -l component=mysql

# services
helm upgrade sky-booking ./config/k8s/helm/service/sky-booking
helm upgrade sky-message ./config/k8s/helm/service/sky-message
helm upgrade sky-notify ./config/k8s/helm/service/sky-notify
helm upgrade sky-offer ./config/k8s/helm/service/sky-offer
