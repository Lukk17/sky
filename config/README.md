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
port forwarding
```
kubectl port-forward <podName> <localPort>:<podPort>
```
where podName is from `kubectl get pod`

example:
```
kubectl port-forward sky-offer-deployment-5796d4f74d-98bs5 5553:5552
```
will be available on the browser:
```
localhost:5553
```

-------------
### Minikube config and dashboard

start minikube with more resources
```
minikube start --cpus 4 --memory 15000
```

minikube visibility under localhost (127.0.0.1") - terminal need to remain open
```
minikube tunnel
```
or else get its address:
```
minikube ip
```

create a tunnel for service:
```
minikube service -n default <serviceName> --url
```
where `serviceName` is from `kubectl get service`
and `default` is namespace


update minikube context:
```
minikube update-context
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
### Kong Install

`kong-cluster-plugins-configuration.yaml` needs to be applied before `kong-custom-resource-definitions.yaml`

kubectl port-forward deployment/ingress-kong -n kong 8444:8444

-------------
### Accessing cluster on local the machine (no Load balancer configured)

There are two ways
1. `minikube tunnel`  
   it will forward traffic to ingresses


2. MetalLB install  - problem with configuring it  
   Use yaml files or install via url (can be found in `installingMetalLB.sh`)
   At beginning, it can fail due to lack of "memberlist" secret, but it will start working in minute


-------------
### Ingress installation: - not required for kong

https://kubernetes.github.io/ingress-nginx/deploy/#quick-start
```
kubectl port-forward --namespace=ingress-nginx service/ingress-nginx-controller 8080:80
```
```
minikube addons enable ingress
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