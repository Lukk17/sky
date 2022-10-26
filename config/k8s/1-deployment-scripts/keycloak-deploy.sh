# service and deployment:
# https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak.yaml

# ingress:
# https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak-ingress.yaml


kubectl apply -f ../api-gateway/keycloak/
kubectl apply -f ../api-gateway/ingress/keycloak-ingress.yaml


# replace hosts with:
# echo keycloak.$(minikube ip).nip.io



#kubectl delete -f ../api-gateway/keycloak/
#kubectl delete -f ../api-gateway/ingress/keycloak-ingress.yaml
