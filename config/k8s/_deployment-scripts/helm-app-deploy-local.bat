:: Will work only if script is run from project main directory with:
:: .\config\k8s\_deployment-scripts\helm-app-deploy.bat

:: sealed secrets
kubectl create namespace sealed-secrets
kubectl create secret tls sealed-secrets-key --cert=.\config\k8s\secret\sealed-public.crt --key=.\config\k8s\secret\sealed-private.key -n sealed-secrets
helm install sealed-secrets-controller .\config\k8s\helm\api-gateway\sealed-secrets-controller\ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller

kubectl apply -f .\config\k8s\secret\sealed\sealed-secrets.yaml
kubectl apply -f .\config\k8s\secret\sealed\sealed-docker-cred.yaml

:: api gateway
helm install oauth2-proxy .\config\k8s\helm\api-gateway\oauth2-proxy\

:: independent services
helm install kafka-service .\config\k8s\helm\kafka\

:: db
helm install database-persistent-volume-claim .\config\k8s\helm\db\database-persistent-volume-claim\
helm install mysql .\config\k8s\helm\db\mysql\
kubectl wait --namespace default --for=condition=ready --timeout=180s pod -l component=mysql

:: services
helm install sky-booking .\config\k8s\helm\service\sky-booking --values .\config\k8s\helm\service\sky-offer\values-local.yaml
helm install sky-message .\config\k8s\helm\service\sky-message --values .\config\k8s\helm\service\sky-offer\values-local.yaml
helm install sky-notify .\config\k8s\helm\service\sky-notify --values .\config\k8s\helm\service\sky-offer\values-local.yaml
helm install sky-offer .\config\k8s\helm\service\sky-offer --values .\config\k8s\helm\service\sky-offer\values-local.yaml

