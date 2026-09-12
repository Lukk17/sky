# Kong ingress (experiment, not in use)

Scratch notes for the Kong manifests in this directory. Kong is not part of the deployed platform and never was.

The edge is nginx-ingress plus oauth2-proxy in the cluster, documented in [config/k8s/helm/helm_README.md](../../helm/helm_README.md), and sky-gateway on port 5777 locally, documented in [sky-gateway/README.md](../../../../sky-gateway/README.md). Read the rest of this file only if you are reviving the Kong experiment.

---

### Kong Install

<br>

https://konghq.com/blog/kubernetes-ingress-api-gateway
https://docs.konghq.com/gateway/3.0.x/get-started/key-authentication/
https://bitnami.com/stack/kong/helm

<br>

#### Firstly, install ingress-nginx or enable plugin in minikube(if not done, readiness probe will fail with code 500):
```bash
minikube addons enable ingress
```
`kong-cluster-plugins-configuration.yaml` needs to be applied before `kong-custom-resource-definitions.yaml`

<br>

#### Port forwarding:
admin(8001, 8444):
```bash
kubectl port-forward deployment/ingress-kong -n kong 8444:8444
```
https://localhost:8444/

manager(8002, 8445):
```bash
kubectl port-forward deployment/ingress-kong -n kong 8445:8445
```
https://localhost:8445/manager

portal(8446):
```bash
kubectl port-forward deployment/ingress-kong -n kong 8446:8446
```
https://localhost:8446/

portal api(8447):
```bash
kubectl port-forward deployment/ingress-kong -n kong 8447:8447
```
https://localhost:8446/

<br>

or only to ingress via:
```bash
minikube tunnel
```
