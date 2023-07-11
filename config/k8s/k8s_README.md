
## Accessing app

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

## Login

Nginx's ingress should catch every unauthenticated request and redirect to login page.
Login page is controlled by oauth2-proxy and will redirect to provider (for example auth0).  

To login via postman with auth0:  
* user/password
https://auth0.com/docs/get-started/authentication-and-authorization-flow/call-your-api-using-resource-owner-password-flow#ask-for-a-token
* auth code flow
https://auth0.com/docs/get-started/authentication-and-authorization-flow/call-your-api-using-the-authorization-code-flow

1. need to add "Username-Password-Authentication" to auth0:
    Dashboard -> setting ->  API Authorization Settings -> in "Default Directory"
   https://stackoverflow.com/questions/69419470/auth0-error-authorization-server-not-configured-with-default-connection

2. Need to add checkbox "Password" under:
   Dashboard -> Application -> <app name> > Scroll down to "Advance Settings" -> Grant types

3. POST to `https://lukk17.eu.auth0.com/oauth/token` with required parameters:
    ```
   curl --request POST \
        --url 'https://{yourDomain}/oauth/token' \
        --header 'content-type: application/x-www-form-urlencoded' \
        --data grant_type=password \
        --data 'username={username}' \
        --data 'password={password}' \
        --data 'audience={yourApiIdentifier}' \
        --data scope=read:sample \
        --data 'client_id={yourClientId}' \
        --data 'client_secret={yourClientSecret}'
   ```
   where:
    * audience - is api identifier (API Audience) in:
    `Dashboard -> Application -> APIs`
    example: `https://lukk17.eu.auth0.com/api/v2/`
    * url - example: `https://lukk17.eu.auth0.com/oauth/token`


Setting postman oauth2 token generation:  
https://community.auth0.com/t/postman-scripts-for-login-using-the-authorization-code-flow-with-pkce/68709


test user (to register):  
email: `lukk@test.com`  
pass: `Test1234!`

--------------

## Kubernetes

### deploy

waiting for deployment to be ready:
```shell
kubectl wait --namespace mynamespace --for=condition=ready --timeout=120s deployment/<deploymentName>

```

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

## Secrets

#### Two ways of creating:
1. By secret file

    ```shell
    kubectl apply -f ./secret.yaml
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

## Kubernetes commands

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

## Troubleshooting

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
