#!/usr/bin/env bash
# Will work only if script is run from project main directory:
#   ./config/k8s/_deployment-scripts/helm/linux/helm-app-deploy.sh
#
# Pass `ENV=prod` (default) to layer the production overlay on top of each service's
# values.yaml. Future env split: pass `ENV=dev` etc. once values-dev.yaml exists.
set -euo pipefail

ENV="${ENV:-prod}"

# sealed secrets
kubectl create namespace sealed-secrets
kubectl create secret tls sealed-secrets-key --cert=./config/k8s/secret/sealed-public.crt --key=./config/k8s/secret/sealed-private.key -n sealed-secrets
helm install sealed-secrets-controller ./config/k8s/helm/api-gateway/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller

kubectl apply -f ./config/k8s/secret/sealed/sealed-secrets.yaml
kubectl apply -f ./config/k8s/secret/sealed/sealed-docker-cred.yaml
kubectl apply -f ./config/k8s/secret/sealed/sealed-dev-ssl-cert.yaml

# api gateway
helm install oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/

# independent services
helm install kafka-service ./config/k8s/helm/kafka/

# db
helm install database-persistent-volume-claim ./config/k8s/helm/db/database-persistent-volume-claim/
helm install mysql ./config/k8s/helm/db/mysql/
kubectl wait --namespace default --for=condition=ready --timeout=180s pod -l component=mysql

# services — each chart's defaults plus the per-env overlay
for svc in sky-booking sky-message sky-notify sky-offer; do
  helm install "$svc" "./config/k8s/helm/service/$svc" \
    -f "./config/k8s/helm/service/$svc/values.yaml" \
    -f "./config/k8s/helm/service/$svc/values-${ENV}.yaml"
done
