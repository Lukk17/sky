### Kong Install

<br>

https://konghq.com/blog/kubernetes-ingress-api-gateway
https://docs.konghq.com/gateway/3.0.x/get-started/key-authentication/
https://bitnami.com/stack/kong/helm

<br>

#### Firstly install ingress-nginx or enable plugin in minikube(if not done readiness probe will fail with code 500):
```
minikube addons enable ingress
```
`kong-cluster-plugins-configuration.yaml` needs to be applied before `kong-custom-resource-definitions.yaml`

<br>

#### Port forwarding:
admin(8001, 8444):
```
kubectl port-forward deployment/ingress-kong -n kong 8444:8444
```
https://localhost:8444/

manager(8002, 8445):
```
kubectl port-forward deployment/ingress-kong -n kong 8445:8445
```
https://localhost:8445/manager

portal(8446):
```
kubectl port-forward deployment/ingress-kong -n kong 8446:8446
```
https://localhost:8446/

portal api(8447):
```
kubectl port-forward deployment/ingress-kong -n kong 8447:8447
```
https://localhost:8446/

<br>

or only to ingress via:
```
minikube tunnel
```
