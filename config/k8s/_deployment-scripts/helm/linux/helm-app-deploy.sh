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

# Sync the canonical realm into the chart. config/keycloak/sky-realm.json is the
# single source of truth; the chart copy is generated here and is gitignored.
cp ./config/keycloak/sky-realm.json ./config/k8s/helm/infra/keycloak/files/sky-realm.json

# Keycloak backing postgres, then Keycloak itself
helm install keycloak ./config/k8s/helm/infra/keycloak/ \
  -f ./config/k8s/helm/infra/keycloak/values.yaml \
  -f "./config/k8s/helm/infra/keycloak/values-${ENV}.yaml"
kubectl wait --namespace default --for=condition=ready --timeout=300s pod -l component=keycloak-postgres
kubectl wait --namespace default --for=condition=ready --timeout=300s pod -l component=keycloak

# api gateway
helm install oauth2-proxy ./config/k8s/helm/api-gateway/oauth2-proxy/

# app database (PostgreSQL)
helm install database-persistent-volume-claim ./config/k8s/helm/db/database-persistent-volume-claim/
helm install postgres ./config/k8s/helm/db/postgres/
kubectl wait --namespace default --for=condition=ready --timeout=180s pod -l component=postgres

# object storage
helm install minio ./config/k8s/helm/infra/minio/
kubectl wait --namespace default --for=condition=ready --timeout=120s pod -l component=minio

# independent services
helm install kafka-service ./config/k8s/helm/kafka/

# services — each chart's defaults plus the per-env overlay
for svc in sky-booking sky-message sky-notify sky-offer; do
  helm install "$svc" "./config/k8s/helm/service/$svc" \
    -f "./config/k8s/helm/service/$svc/values.yaml" \
    -f "./config/k8s/helm/service/$svc/values-${ENV}.yaml"
done
