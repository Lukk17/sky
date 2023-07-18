:: Will work only if script is run from project main directory with:
:: .\config\k8s\_deployment-scripts\kube-app-remove.bat

:: sealed secrets
kubectl delete namespace sealed-secrets
kubectl delete secret sealed-secrets-key -n sealed-secrets
kubectl delete -f .\config\k8s\secret\sealed-secrets-controller.yaml -n sealed-secrets
kubectl delete -f .\config\k8s\secret\sealed\sealed-secrets.yaml
kubectl delete -f .\config\k8s\secret\sealed\sealed-docker-cred.yaml

:: api gateway
kubectl delete -f .\config\k8s\vanilla\api-gateway\ingress\ingress.yaml
kubectl delete -f .\config\k8s\vanilla\api-gateway\oauth2-proxy\

:: independent services
kubectl delete -f .\config\k8s\vanilla\kafka\

:: db
kubectl delete -f .\config\k8s\vanilla\db\mysql\

:: services
kubectl delete -f .\config\k8s\vanilla\service\sky-booking\
kubectl delete -f .\config\k8s\vanilla\service\sky-message\
kubectl delete -f .\config\k8s\vanilla\service\sky-notify\
kubectl delete -f .\config\k8s\vanilla\service\sky-offer\
