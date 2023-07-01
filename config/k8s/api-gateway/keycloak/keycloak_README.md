# Keycloak

## Deployment

<br>

#### Run deployment script:
`keycloak-deploy.sh`  

or

```shell
kubectl apply -f ./keycloak-deployment.yaml
kubectl apply -f ./keycloak-service.yaml
kubectl apply -f ./keycloak-ingress.yaml
```
which base on:

service and deployment:  
https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak.yaml

ingress:  
https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak-ingress.yaml

<br>

## Config

#### Replace ingress keycloak IP address with current minikube
in:
`ingress.yaml`
```shell
nginx.ingress.kubernetes.io/auth-url:
nginx.ingress.kubernetes.io/auth-signin:
```

<br>

#### Replace both hosts (tls.hosts and rules.host)
in:  
`keycloak-ingress.yaml`  
with:
```shell
echo keycloak.$(minikube ip).nip.io
```

<br>

#### Set up realms:
https://www.keycloak.org/getting-started/getting-started-kube

<br>

Admin:
```shell
keycloak.< minikubeIP >.nip.io/admin/master/console
```
Account:
```shell
keycloak.< minikubeIP >.nip.io/realms/sky/account  
```

<br>

console login - setup in secret.yaml keycloakuser and keycloakpass (user: superSecureName!)
can be decoded using:
https://www.base64decode.org/

<br>

Creating realm:
1. Open the Keycloak Admin Console.
2. Click the word master in the top-left corner, then click Create realm.
3. Enter `sky` in the Realm name field.
4. Click Create.

Create a user:
1. Click Users in the left-hand menu.
2. Click Create new user.
3. Fill in the form with the following values:
   Username: `lukk`  
   Email: `lukk@test.com`  
   First name: `Lukk`  
   Last name: `Test`
4. Click Create.

Set password:
1. Click Credentials at the top of the page.
2. Fill in the Set password form with `test1234`.
3. Toggle Temporary to Off so that the user does not need update this password at the first login.

Check if log in to the Account Console is working
1. Go to:
   `keycloak.<minikube IP>.nip.io/realms/sky/account`
2. log in using created account (`lukk`:`test1234`)

Secure application:
1. Open the Keycloak Admin Console.
2. Click Clients.
3. Click Create client
4. Fill in the form with the following values:
5. Client type: OpenID Connect
6. Client ID: `skyclient`
7. Click Next
8. Confirm that Standard flow is enabled.
9. Click Save.

After the client is created, make these updates to the client:
1. Scroll down to Access settings.
2. Set Valid redirect URIs to https://www.keycloak.org/app/*
3. Set Web origins to https://www.keycloak.org
4. Click Save.

<br>

Other installation options:  
https://bitnami.com/stack/keycloak/helm
https://www.keycloak.org/keycloak-benchmark/kubernetes-guide/latest/installation

<br>

-----------------

## GCP
After deployment on GCP:
https://console.cloud.google.com/kubernetes/discovery?

in "Services & Ingress" you can find link to external endpoint <cluster ip>:<port>
```shell
kubectl get svc
```
you will get:
```shell
NAME                                 TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)                      AGE
ingress-nginx-controller             LoadBalancer   10.121.2.200   34.118.116.39   80:31460/TCP,443:31806/TCP   16m
ingress-nginx-controller-admission   ClusterIP      10.121.1.245   <none>          443/TCP                      16m
keycloak                             LoadBalancer   10.121.3.152   34.116.246.43   8080:31968/TCP               124m

```
nginx: 34.118.116.39:80
keycloak: 34.116.246.43:8080


## Troubleshooter


#### 404 not found on keycloak

If above configuration not working this can be tried:
To check correct keycloak address paste this command in terminal:
```shell
KEYCLOAK_URL=http://$(minikube ip):$(kubectl get services/keycloak -o go-template='{{(index .spec.ports 0).nodePort}}') &&
echo "" &&
echo "Keycloak:                 $KEYCLOAK_URL" &&
echo "Keycloak Admin Console:   $KEYCLOAK_URL/admin" &&
echo "Keycloak Account Console: $KEYCLOAK_URL/realms/myrealm/account" &&
echo ""
```

Admin console
< minikubeIP>:< PORT>/admin

Account:
< minikubeIP>:< PORT>/realms/skyrealm/account

But it is common that this way it will not be working (stuck on loading screen)
