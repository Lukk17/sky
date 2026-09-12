# Operating the cluster

kubectl reference for a sky cluster that is already running: how to reach the app, inspect it, scale it, and debug it.

Deploying in the first place is [config/k8s/_deployment-scripts/deployment_README.md](_deployment-scripts/deployment_README.md). What each chart contains is [config/k8s/helm/helm_README.md](helm/helm_README.md). Standing up a local cluster is [config/k8s/local_README.md](local_README.md).

---

### Reaching the app

The deployed platform answers at [https://skycloud.luksarna.com](https://skycloud.luksarna.com). To find the load balancer address directly, list the ingress controller service:

```shell
kubectl get svc -n ingress-nginx
```

```text
NAME                                 TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)
ingress-nginx-controller             LoadBalancer   10.121.2.200   34.118.116.39   80:31460/TCP,443:31806/TCP
ingress-nginx-controller-admission   ClusterIP      10.121.1.245   <none>          443/TCP
```

`EXTERNAL-IP` on the first row is the address the DNS record points at. On GKE the same information is under Services and Ingress in the [Kubernetes console](https://console.cloud.google.com/kubernetes/discovery).

`sky-notify` publishes its WebSocket endpoint through the same load balancer: its chart templates an Ingress on `/notifyWebsocket`, so a browser connects at `wss://skycloud.luksarna.com/notifyWebsocket`. Port forwarding, below, is a debugging route rather than the only way in.

---

### Swagger

Three services publish Swagger UI through the ingress. All three sit behind oauth2-proxy, so a browser session is required.

| Service | URL |
|---|---|
| sky-offer | https://skycloud.luksarna.com/offer/swagger-ui/index.html |
| sky-booking | https://skycloud.luksarna.com/booking/swagger-ui/index.html |
| sky-message | https://skycloud.luksarna.com/msg/swagger-ui/index.html |

The reverse proxy rewrites the path, and Swagger UI cannot always work out where its own config lives behind that rewrite. When the page loads empty, paste the matching api-docs URL into the Explore box:

| Service | api-docs URL |
|---|---|
| sky-offer | https://skycloud.luksarna.com/offer/v3/api-docs/public |
| sky-booking | https://skycloud.luksarna.com/booking/v3/api-docs/public |
| sky-message | https://skycloud.luksarna.com/msg/v3/api-docs/public |

---

### Logging in

nginx catches every unauthenticated request to a protected path and redirects it to oauth2-proxy, which runs the OIDC flow against Keycloak on the `sky` realm. After a browser login the session cookie is:

```text
_oauth2_proxy=<token>
```

To drive authentication from a REST client instead of a browser, use the Keycloak password grant directly rather than the browser redirect flow. The command, the client id, and the demo users are in [config/keycloak/SETUP.md](../keycloak/SETUP.md), and the Bruno collection at [docs/api/request/](../../docs/api/request/) already automates it.

---

### Status

```shell
kubectl get pods
```

```shell
kubectl get pods -o wide
```

```shell
kubectl get services
```

```shell
kubectl get deployments
```

```shell
kubectl get pv
```

```shell
kubectl get pvc
```

---

### Detail on one object

```shell
kubectl describe pod <pod-name>
```

Pod names come from `kubectl get pods`. `describe` takes any object type, so `kubectl describe ingress sky-offer-ingress` and `kubectl describe statefulset postgres-deployment` work the same way.

---

### Logs

```shell
kubectl logs <pod-name>
```

Follow a live stream:

```shell
kubectl logs -f <pod-name>
```

Read the logs of the previous container after a crash loop:

```shell
kubectl logs --previous <pod-name>
```

---

### Waiting for a rollout

```shell
kubectl rollout status deployment/sky-offer-deployment
```

```shell
kubectl wait --for=condition=ready --timeout=180s pod -l app=sky-offer
```

---

### Restarting and updating

Restart a deployment in place, which is what you want after importing a rebuilt image with the same tag:

```shell
kubectl rollout restart deployment/sky-offer-deployment
```

Point a deployment at a different image without touching the chart. Carry the same leading `v` the published tag has, because a reference without it names a tag that was never pushed:

```shell
kubectl set image deployment/sky-offer-deployment sky-offer-container=lukk17/sky-offer:v1.0.1
```

That change is lost on the next `helm upgrade`. Bump `deployment.image.tag` in the chart values for anything permanent, which is where the tag a deployed release actually runs is pinned.

Roll back a bad rollout:

```shell
kubectl rollout undo deployment/sky-offer-deployment
```

---

### Scaling

```shell
kubectl scale deployment/sky-offer-deployment --replicas=3
```

---

### Port forwarding

Reach a pod's port on localhost, which bypasses the ingress and the oauth2-proxy session, so it is useful for talking to a service directly while debugging:

```shell
kubectl port-forward deployment/sky-notify-deployment 5554:5554
```

The same works for the database:

```shell
kubectl port-forward statefulset/postgres-deployment 5432:5432
```

And for floci, the object store, whose S3 API is one port:

```shell
kubectl port-forward statefulset/floci-deployment 4566:4566
```

---

### Deleting resources

```shell
kubectl delete pod <pod-name>
```

```shell
kubectl delete -f <file or directory>
```

Deleting a pod that belongs to a Deployment or StatefulSet only makes the controller create a new one, which is a legitimate way to force a restart. To actually remove a service, uninstall its Helm release instead.

---

### Secrets

The cluster reads one Secret, `sky-secrets`. Its keys and what reads each of them are in [config/k8s/helm/helm_README.md](helm/helm_README.md). Creating and sealing it is in [config/k8s/_deployment-scripts/deployment_README.md](_deployment-scripts/deployment_README.md).

Check that a key is present without printing its value:

```shell
kubectl get secret sky-secrets -o jsonpath='{.data}'
```

Base64-encode a value by hand when writing a plain Secret manifest:

```shell
echo -n '<value>' | base64
```

---

### Troubleshooting

Start a throwaway pod inside the cluster network, which is the quickest way to test whether one service can reach another:

```shell
kubectl run -it --rm debug --image=busybox --restart=Never -- sh
```

From inside it, `wget -qO- http://sky-offer-service:5552/actuator/health` proves in-cluster DNS and the service port at once.

Open a psql session against the app database:

```shell
kubectl exec -it statefulset/postgres-deployment -- psql -U postgres -d sky
```

Useful once you are in: `\dt` lists the tables, and `select * from flyway_schema_history_offer order by installed_rank desc limit 5;` shows what Flyway applied last for one service. Each stateful service keeps its own history table in the shared `sky` database.

Open a shell in any pod:

```shell
kubectl exec -it <pod-name> -- /bin/sh
```

---

### Docs map

| Document | What it covers |
|---|---|
| [README.md](../../README.md) | Platform overview, modules, build, ports |
| [config/k8s/_deployment-scripts/deployment_README.md](_deployment-scripts/deployment_README.md) | Deploying to the GCP cluster, sealed secrets, deployment scripts |
| [config/k8s/helm/helm_README.md](helm/helm_README.md) | Chart-by-chart reference, secret key inventory, upgrades |
| [config/k8s/local_README.md](local_README.md) | Local Kubernetes cluster on k3d: bring-up, verification, teardown |
| [config/local-dev/local_README.md](../local-dev/local_README.md) | Running locally without Kubernetes: Gradle and Docker Compose |
| [config/keycloak/SETUP.md](../keycloak/SETUP.md) | Keycloak realm, import, certificate trust, users, tokens |
| [docs/api/README.md](../../docs/api/README.md) | Bruno collection and OpenAPI specs |
