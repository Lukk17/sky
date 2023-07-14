# K8S

## 1. Accessing app

App should be accessible under its domain:  
`sky.luksarna.com`
or you can find its ip on GKE:
https://console.cloud.google.com/kubernetes/discovery

in "Services & Ingress" you can find link to external endpoint <cluster ip>:<port>
```shell
kubectl get svc
```
you will get:
```shell
NAME                                 TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)                      AGE
ingress-nginx-controller             LoadBalancer   10.121.2.200   34.118.116.39   80:31460/TCP,443:31806/TCP   16m
ingress-nginx-controller-admission   ClusterIP      10.121.1.245   <none>          443/TCP                      16m             124m

```
nginx external IP - under this IP app is running 
```
34.118.116.39:80
```

--------------

## 2. Login

Nginx's ingress should catch every unauthenticated request and redirect to login page.
Login page is controlled by oauth2-proxy and will redirect to provider (for example auth0).  

### Easiest

Login in browser and copy cookie:
```
_oauth2_proxy=<token>
```

test user (to register):  
email: `lukk@test.com`  
pass: `Test1234!`

### Rest way: 
[Postman rest](#auth0-login-rest-way)

--------------

## 3. Secrets

### Encrypted using Sealed Secrets

Install instruction can be found [here](/k8s/_deployment-scripts/deployment_README.md)

Save Public Key Locally:
```shell
kubeseal --fetch-cert --controller-name=sealed-secrets --controller-namespace=default > <sealed-secrets.-cert-name>.pem
```
Seal the Secret:
```shell
kubeseal --format=yaml --cert=<sealed-secrets.-cert-name>.pem < <kubernetes-secret-file>.yaml > <sealed-secret-file>.yaml
```
Apply the Sealed Secret:
```shell
kubectl apply -f <sealed-secret-file>.yaml
```

-------------

### Pure kubernetes
[Only base64 encoded](#pure-kubernetes-only-base64-encoded)

### Creating secret from generated auth file
[See this](#secret-from-generated-auth-file)

-------------
## 4. Kubernetes

### Deploy

[README](/k8s/_deployment-scripts/deployment_README.md)

Waiting for deployment to be ready:
```shell
kubectl wait --namespace mynamespace --for=condition=ready --timeout=120s deployment/<deploymentName>

```

--------------


### Port forwarding
```shell
kubectl port-forward <podName> <localPort>:<podPort>
```
where podName is from `kubectl get pod`

example:
```shell
kubectl port-forward sky-offer-deployment-5796d4f74d-98bs5 5553:5552
```

will be available on the browser:
```
localhost:5553
```

-------------


## 5. Kubernetes commands

### Status:

```shell
kubectl get pods
```
more info:
```shell
kubectl get pods -o wide
```

```shell
kubectl get services
```
```shell
kubectl get deployments
```


Persistent volume info:
```shell
kubectl get pv
```
```shell
kubectl get pvc
```
-------------
###  Details of pod:


```shell
kubectl describe <object type> <name>
```
where object type can be:
* pod (or pods)
  name - name of object (pod)


Update deployment with a new image:
```shell
kubectl set image <object type>/<object name> <container name>=<full image name>
```

example:
```shell
kubectl set image deployment/auth-deployment auth=lukk17/sky-auth:1.0.0
```
-------------
###  Logs of pod:


```shell
kubectl logs <kubeName>
```
kubeName can be obtained from `kubectl get pods`

-------------
### Scaling

```shell
kubectl scale --replicas=<number> <name>
kubectl scale --replicas=<number> -f <deployment file>
```

-------------
### Deleting resource

```shell
kubectl delete pod,service <name>
kubectl delete -f <file or folder>
kubectl -n <namespace> delete pod,svc --all
```

-------------

## 6. Troubleshooting

### MYSQL configuration

#### Exec into mysql container (easy via minikube dashboard)
or use command:
```shell
kubectl exec --stdin --tty <podFullName> -- /bin/bash
```
where podFullName can be obtained from `kubectl get pod`

#### Making root for all host, not only localhost:
```mysql
USE mysql;
UPDATE user SET host='%' WHERE host='localhost'
```

-------------

### Pure kubernetes (only base64 encoded):
1. By secret file

    ```shell
    kubectl apply -f ./secrets.yaml
    ```
   where secrets are coded by base64:
    ```shell
    echo -n '<dataToBeCoded>' | base64
    ```

2. Needs to be created manually on machine by terminal

    ```shell
    kubectl create secret generic <secretName> --from-literal <secretName>=<secret>
    ```
   example:
    ```shell
    kubectl create secret generic mysqlRootPass --from-literal mysqlRootPass=elasticPass!
    ```


-------------
### Secret from generated auth file

Generate auth file with secrets (must be generated in same folder):
```shell
htpasswd -c auth <username>
```
Creating secret from generated file:
```shell
kubectl create secret generic basic-auth --from-file=.\config\k8s\api-gateway\ingress\auth
```

Deleting secret:
```shell
kubectl delete secret basic-auth
```

-------------


### Auth0 login Rest way

To login via postman with auth0:
https://community.auth0.com/t/full-auth-code-flow-using-postman/105024

Request are in postman collection config/postman-collection/sky.postman_collection.json  
inside cloud/Auth0 Code Flow

IMPORTANT : clear cookies in postman (if not then Bad request error occurs)  

Go step by step:
1. Get to /authorize
2. Post to /u/login
3. Get to /resume
4. Exchange CODE against a TOKEN

--------------

### Setting account role to "Kubernetes Engine Admin"

To be able to set up service accounts (required by sealed secrets)

1. Go to the IAM & Admin page in Google Cloud Console.
2. Click on "IAM".
3. Click "ADD" at the top of the page to add a new member.
4. In the "New members" field, type the email address of the user.
5. In the "Role" dropdown, select "Kubernetes Engine" -> "Kubernetes Engine Admin".
6. Click "SAVE".
