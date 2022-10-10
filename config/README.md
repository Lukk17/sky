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
kubectl apply -f config/k8s --recursive
```

-------------
### Ingress installation:

https://kubernetes.github.io/ingress-nginx/deploy/#quick-start
```
kubectl port-forward --namespace=ingress-nginx service/ingress-nginx-controller 8080:80
```
```
minikube addons enable ingress
```

-------------
### Minikube config and dashboard

minikube visibility under localhost (127.0.0.1") - terminal need to remain open
```
minikube tunnel
```
or else get its address:
```
minikube ip
```

minikube dashboard - terminal need to remain open
```
minikube dashboard
```
or just url address :
```
minikube dashboard --url
```
-------------
### MYSQL configuration
exec into mysql container (easy via minikube dashboard)
or use command:
```
kubectl exec --stdin --tty <podFullName> -- /bin/bash
```
where podFullName can be obtained from `kubectl get pod`

inside pod log into mysql:
```
mysql -u root -p
```
then create sky db:
```
CREATE DATABASE IF NOT EXISTS sky;
```
now restart services

after that insert example values stored in script/sql_commands:
`sql_roles_insert.sql`
then register admin and use:
`sql_user_role_admin.sql`
and then use rest of them.

-------------
### Secrets  

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
### Status:
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
###  Details of pod:
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
###  Logs of pod:
```
kubectl logs <kubeName>
```
kubeName can be obtained from `kubectl get pods`