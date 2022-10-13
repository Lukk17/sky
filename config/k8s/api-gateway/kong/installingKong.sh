# use yaml files in project to create kong
# other option is using url to install it (or with newer version):
kubectl apply -f https://github.com/Kong/kubernetes-ingress-controller/blob/main/deploy/single/all-in-one-dbless.yaml
#it can fail due to lack of KONG_DECLARATIVE_CONFIG files in deployment
kubectl apply -f https://github.com/robincher/kong-oidc-keycloak-boilerplate/blob/master/kubernetes/kong-deployment.yaml
kubectl apply -f https://github.com/robincher/kong-oidc-keycloak-boilerplate/blob/master/kubernetes/kong-crds.yaml