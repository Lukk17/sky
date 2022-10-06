## Kubernetes

Start pods from configuration files:

```
kubectl apply -f auth-service.yaml
```
```
kubectl apply -f auth-deployments.yaml
```
Simpler, you can run all scripts in a folder (in terminal being in parent folder):
```
kubectl apply -f auth-service
```
-------------
Secrets  

Two ways of creating:
1. By secret file

```
kubectl apply -f ./secret.yaml
```
where secrets are coded by base64:
```
echo -n '<dataToBeCoded>' | base64
```

2. Needs to be created manually on machine by terminal

```
kubectl create secret generic <secretName> --from-literal <secretName>=<secret>
```
example:
```
kubectl create secret generic mysqlRootPass --from-literal mysqlRootPass=elasticPass!
```

-------------
Get status:
```
kubectl get pods
```
more info:
```
kubectl get pods -o wide
```

```
kubectl get services
```
```
kubectl get deployments
```

Persistent volume info:
```
kubectl get pv
```
```
kubectl get pvc
```
-------------
Get details of pod:
```
kubectl describe <object type> <name>
```
where object type can be:
* pod (or pods)
  name - name of object (pod)

update deployment with a new image:
```
kubectl set image <object type>/<object name> <container name>=<full image name>
```
example:
```
kubectl set image deployment/auth-deployment auth=lukk17/sky-auth:1.0.0
```
-------------
Get logs of pod:
```
kubectl logs <kubeName>
```
kubeName can be obtained from `kubectl get pods`

-------------
To be able to access pod in browser IP address of minikube is needed:
```
minikube ip
```