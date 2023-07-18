:: Will work only if script is run from project main directory with:
:: .\config\k8s\_deployment-scripts\kube-app-deploy.bat

:: sealed secrets
kubectl create namespace sealed-secrets
kubectl create secret tls sealed-secrets-key --cert=.\config\k8s\secret\sealed-public.crt --key=.\config\k8s\secret\sealed-private.key -n sealed-secrets
kubectl apply -f .\config\k8s\secret\sealed-secrets-controller.yaml -n sealed-secrets
kubectl apply -f .\config\k8s\secret\sealed\sealed-secrets.yaml
kubectl apply -f .\config\k8s\secret\sealed\sealed-docker-cred.yaml

:: api gateway
kubectl apply -f .\config\k8s\vanilla\api-gateway\ingress\ingress.yaml
kubectl apply -f .\config\k8s\vanilla\api-gateway\oauth2-proxy\

:: independent services
kubectl apply -f .\config\k8s\vanilla\kafka\

:: db
kubectl apply -f .\config\k8s\vanilla\db\mysql\
kubectl wait --namespace default --for=condition=ready --timeout=120s pod -l component=mysql

:: services
kubectl apply -f .\config\k8s\vanilla\service\sky-booking\
kubectl apply -f .\config\k8s\vanilla\service\sky-message\
kubectl apply -f .\config\k8s\vanilla\service\sky-notify\
kubectl apply -f .\config\k8s\vanilla\service\sky-offer\
