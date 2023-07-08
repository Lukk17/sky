# service and deployment:
# https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak.yaml
# https://github.com/keycloak/keycloak-quickstarts/blob/latest/kubernetes-examples/keycloak.yaml

# ingress:
# https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak-ingress.yaml
# https://github.com/keycloak/keycloak-quickstarts/blob/latest/kubernetes-examples/keycloak-ingress.yaml


kubectl apply -f ./keycloak-deployment.yaml
kubectl apply -f ./keycloak-service.yaml
kubectl apply -f ./keycloak-ingress.yaml

# replace hosts with:
# echo keycloak.$(minikube ip).nip.io


#kubectl delete -f ./keycloak-deployment.yaml
#kubectl delete -f ./keycloak-service.yaml
#kubectl delete -f ./keycloak-ingress.yaml
