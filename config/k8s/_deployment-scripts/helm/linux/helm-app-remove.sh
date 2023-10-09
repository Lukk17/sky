# Will work only if script is run from project main directory with:
# ./config/k8s/_deployment-scripts/helm/linux/helm-app-remove.sh

# sealed secrets
kubectl delete namespace sealed-secrets
kubectl delete secret sealed-secrets-key -n sealed-secrets
helm uninstall sealed-secrets-controller -n sealed-secrets

kubectl delete -f ./config/k8s/secret/sealed/sealed-secrets.yaml
kubectl delete -f ./config/k8s/secret/sealed/sealed-docker-cred.yaml
kubectl delete -f ./config/k8s/secret/sealed/sealed-dev-ssl-cert.yaml

# api gateway
helm uninstall oauth2-proxy

# independent services
helm uninstall kafka-service
kubectl delete pvc data-kafka-service-0

# db
helm uninstall mysql

# services
helm uninstall sky-booking
helm uninstall sky-message
helm uninstall sky-notify
helm uninstall sky-offer
