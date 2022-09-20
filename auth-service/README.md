# Authentication service for Sky 

<br>

port: 9100
<br>
name: auth-service

---------------------------------------------
kubernetes
start from configuration files:
```
kubectl apply -f auth-pod.yaml
```
```
kubectl apply -f auth-service.yaml
```
```
kubectl apply -f auth-deployments.yaml
```

get status:
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

get details of pod:
```
kubectl describe <object type> <name>
```
where object type can be:
 * pod (or pods)
name - name of object (pod)
   
update deployment with new image:
```
kubectl set image <object type>/<object name> <container name>=<full image name>
```
example:
```
kubectl set image deployment/auth-deployment auth=lukk17/sky-auth:1.0.0
```


to be able to access pod in browser IP address of minikube is needed:
```
minikube ip
```