## Kubernetes

<br>

#### Start pods from configuration files:

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

<br>

#### Port forwarding
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

<br>

#### Start minikube with more resources
```
minikube start --cpus 4 --memory 16384
```
on Windows, it needs setup in wsl - by creating `.wslconfig` file in home directory:
```
[wsl2]
memory=20GB   # Limits VM memory in WSL 2 up to 3GB
processors=4 # Makes the WSL 2 VM use two virtual processors
```
or set parameters before:
```
minikube config set memory 12288
minikube config set cpus 4
minikube config set disk-size 15000
```

<br>

#### If error with pulling docker images:

pull images with this command:
```
minikube ssh docker pull <imageName>
```
examples:
```
minikube ssh docker pull mysql
minikube ssh docker pull quay.io/keycloak/keycloak:19.0.3
minikube ssh docker pull lukk17/sky-offer
minikube ssh docker pull lukk17/sky-message
```

<br>

#### If not working start in containerd runtime:
```
minikube start --container-runtime=containerd
```
or via setting parameter:
```
minikube config set container-runtime containerd
```
Valid options: docker, cri-o, containerd (default: auto)

Sometimes change of driver helps:
```
minikube start --driver=none
```

<br>

#### If not working try cache images:
```
minikube cache add <dockerhub username>/<repo name>:<version optional>
```
due to cache deprecation best to use:
```
minikube image load <dockerhub username>/<repo name>:<version optional>
```
AND add in deployments definition:  
```
spec:
    template:
        spec:
            containers:
                imagePullPolicy: Never
```

examples:
```
minikube image load mysql
minikube image load quay.io/keycloak/keycloak:19.0.3
minikube image load lukk17/sky-offer
minikube image load lukk17/sky-message
```

additional thing to try:
```
docker pull <imageName>
```
<br>

#### minikube visibility under localhost (127.0.0.1") - terminal need to remain open
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

<br>

#### Update minikube context:
```
minikube update-context
```

<br>

#### minikube dashboard - terminal need to remain open
```
minikube dashboard
```
or just url address :
```
minikube dashboard --url
```

-------------
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

-------------
### Keycloak installation

<br>

https://bitnami.com/stack/keycloak/helm
https://www.keycloak.org/keycloak-benchmark/kubernetes-guide/latest/installation

-------------

### Secrets

<br>

#### Two ways of creating:
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
### MYSQL configuration

<br>

#### Exec into mysql container (easy via minikube dashboard)
or use command:
```
kubectl exec --stdin --tty <podFullName> -- /bin/bash
```
where podFullName can be obtained from `kubectl get pod`

<br>

#### Making root for all host, not only localhost:
```
use mysql;
update user set host='%' where host='localhost'
```

<br>

#### Creating database (should be already created with configMap):
```
mysql -u root -p
```
then create sky db:
```
CREATE DATABASE IF NOT EXISTS sky;
```
now restart services

<br>

after that insert example values stored in script/sql_commands:  
`sql_roles_insert.sql`  
then register admin and use:  
`sql_user_role_admin.sql`  
and then use rest of them.

-------------
### Accessing cluster on local the machine (no Load balancer configured)

<br>

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

<br>

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

<br>

Persistent volume info:
```
kubectl get pv
```
```
kubectl get pvc
```
-------------
###  Details of pod:

<br>

```
kubectl describe <object type> <name>
```
where object type can be:
* pod (or pods)
  name - name of object (pod)

<br>

Update deployment with a new image:
```
kubectl set image <object type>/<object name> <container name>=<full image name>
```
example:
```
kubectl set image deployment/auth-deployment auth=lukk17/sky-auth:1.0.0
```
-------------
###  Logs of pod:

<br>

```
kubectl logs <kubeName>
```
kubeName can be obtained from `kubectl get pods`