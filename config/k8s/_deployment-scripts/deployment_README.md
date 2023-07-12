# DEPLOYMENT

----------------------

## Docker build and publish

### Build:
```
docker build . -f sky-booking/docker/Dockerfile -t sky-booking:latest --no-cache
docker build . -f sky-offer/docker/Dockerfile -t sky-offer:latest --no-cache
docker build . -f sky-notify/docker/Dockerfile -t sky-notify:latest --no-cache
docker build . -f sky-message/docker/Dockerfile -t sky-message:latest --no-cache

```  

### Push into repository:
```shell
docker tag sky-booking:latest lukk17/sky-booking:latest
docker push lukk17/sky-booking:latest

docker tag sky-offer:latest lukk17/sky-offer:latest
docker push lukk17/sky-offer:latest

docker tag sky-notify:latest lukk17/sky-notify:latest
docker push lukk17/sky-notify:latest

docker tag sky-message:latest lukk17/sky-message:latest
docker push lukk17/sky-message:latest
```

----------------------

## App deployment


----------------------

### Sealed secret

To install `kubeseal` on system see [this](#install-sealed-secret-on-system).  
To deploy standard Kubernetes base64 encoded secrets see [this](#kubernetes-basic-base64-encode-secrets-apply).

To create new sealed secrets see [this](#create-new-sealed-secrets).


```shell
  kubectl apply -f config/k8s/secret/sealed/sealed-secret.yaml
  kubectl apply -f config/k8s/secret/sealed/sealed-docker-cred.yaml
```

----------------------

### Deploy services

```shell
kubectl create secret generic basic-auth --from-file=./api-gateway/ingress/auth
kubectl apply -f config/k8s/api-gateway/ingress/ingress.yaml
kubectl apply -f config/k8s/api-gateway/oauth2-proxy/

kubectl apply -f config/k8s/kafka/

kubectl apply -f config/k8s/db/mysql/
kubectl wait --namespace default --for=condition=ready --timeout=120s deployment/mysql-deployment

kubectl apply -f config/k8s/service/sky-offer/
kubectl apply -f config/k8s/service/sky-booking/
kubectl apply -f config/k8s/service/sky-notify/
kubectl apply -f config/k8s/service/sky-message/
```

Simpler, you can run all scripts in a folder (in terminal being in parent folder):
```shell
kubectl apply -f config/k8s/api-gateway --recursive
kubectl apply -f config/k8s/db --recursive
kubectl wait --namespace default --for=condition=ready --timeout=120s deployment/mysql-deployment
kubectl apply -f config/k8s/kafka --recursive
kubectl apply -f config/k8s/service --recursive
```

----------------------

### Use 

Save Public Key Locally:
```shell
kubeseal --fetch-cert --controller-name=sealed-secrets-controller --controller-namespace=default > <sealed-secrets.-cert-name>.pem
```
Seal the Secret:
```shell
kubeseal --format=yaml --cert=<sealed-secrets.-cert-name>.pem < <kubernetes-secret-file>.yaml > <sealed-secret-file>.yaml
```
Apply the Sealed Secret:
```shell
kubectl apply -f <sealed-secret-file>.yaml
```

----------------------

## GCP

1. Install gcloud CLI  
https://cloud.google.com/sdk/gcloud?authuser=1  
and plugin:
    ```shell
    sudo apt-get install google-cloud-sdk-gke-gcloud-auth-plugin
    ```

2. login into gcloud account
   ```shell
   gcloud auth login
   ```
   
3. Set project
    ```shell
    gcloud config set project sky-app-17
    ```

4. Create cluster (if not existing)
    ```shell
    gcloud container clusters create-auto sky-cluster --location=europe-central2
    ```

5. Get authentication credentials for the cluster - required to interact with cluster
   After this it will be visible in lens
   ```shell
   gcloud container clusters get-credentials sky-cluster --location=europe-central2
   ```

6. Install nginx ingress
   ```shell
   helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
   helm repo update
   helm install ingress-nginx ingress-nginx/ingress-nginx  
   ```

----------------------

## Auth service
1. Creating project in auth0 for authentication  
   https://manage.auth0.com/dashboard
<br>  
   redirect_uri have to be in "Allowed callback URLs" in application settings - in form of:  
   `https://<website address>/oauth2/callback`, example:
   ```
   https://sky.luksarna.com/oauth2/callback
   ```
   test user (to register):  
   email: `lukk@test.com`  
   pass: `Test1234!`  
<br>
2. Create GCP oauth credential under "APIs & Services"
   https://console.cloud.google.com/apis/credentials  
<br>
3. Changing clientId, clientSecret and domain in oauth2 deployment config  
   https://kubernetes.github.io/ingress-nginx/examples/auth/oauth-external-auth/

Adding GitHub login:  
https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app

Adding Google login:
https://developers.google.com/identity/sign-in/web/sign-in

----------------------

### Install sealed secret on system

#### Linux
```shell
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.16.0/kubeseal-linux-amd64 -O kubeseal
sudo install -m 755 kubeseal /usr/local/bin/kubeseal
```

#### Windows
1. Download `kubeseal-0.22.0-windows-amd64.tar.gz` from https://github.com/bitnami-labs/sealed-secrets/releases/tag/v0.22.0
   (or newer version)
2. Extract it and put in install folder, example: `D:\Development\SDK\kubeseal`
3. Add to path folder `D:\Development\SDK\kubeseal\` where `kubeseal.exe` is located.

----------------------

### Deploy sealed secrets controller

By default, it tries to use `kube-system` namespace which can't be used on GKE. 
You need to change namespace in yaml file and add creating of new namespace:
```yaml
---
kind: Namespace
apiVersion: v1
metadata:
  name: sealed-secrets
```

#### Local version (copied online version v0.22.0):
```shell
kubectl apply -f config/k8s/secret/sealed-secrets-controller.yaml
```

#### Online version
```shell
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.22.0/controller.yaml
```
----------------------

### Create new sealed secrets

1. Deploy sealed secrets controller  
   See [this](#deploy-sealed-secrets-controller) for different or online version.
   This one have added namespace "sealed-secrets" and changed in .yaml file.
   ```shell
   kubectl apply -f config/k8s/secret/sealed-secrets-controller.yaml -n sealed-secrets
   ``` 
2. Create secrets.yaml (do not ad to git - should be in .gitignore)
   Keep it only locally as it have base64 encoded password, easy to decode.

   secrets.yaml should look like:
   ```yaml
   ---
   apiVersion: v1
   kind: Secret
   metadata:
   #  needs to be lowercase !
     name: sky-secrets
     namespace: default
   type: Opaque
   data:
     mysql-root-user: <root-user>
     mysql-root-pass: <root-pass>
     mysql-username: <username>
     mysql-password: <password>
     spring-security-user: <security-user>
     spring-security-pass: <security-pass>
     auth0-client-id: <client-id>
     auth0-client-secret: <client-secret>
     auth0-client-cookie-secret: <cookie-secret>
   ```

3. Create docker.cred.yml  (do not ad to git - should be in .gitignore)
   Keep it only locally as it have base64 encoded password, easy to decode.

   docker-cred.yaml should look like:
   ```yaml
   apiVersion: v1
   kind: Secret
   metadata:
      name: docker-cred
   type: kubernetes.io/dockerconfigjson
   stringData:
      docker-server: "<your-registry-server>"
      docker-username: "<your-name>"
      docker-password: "<your-password>"
   ```
4. Fetch cert:
   ```shell
   kubeseal --fetch-cert --controller-name=sealed-secrets-controller --controller-namespace=sealed-secrets > config/k8s/secret/sky-sealed-secrets.pem
   ```
5. Create sealed secrets form normal ones:
   Linux:
   ```shell
   kubeseal --format=yaml --cert=config/k8s/secret/sky-sealed-secrets.pem < config/k8s/secret/secret.yaml > config/k8s/secret/sealed/sealed-secret.yaml
   ```
   Windows:
   ```powershell
   Get-Content config/k8s/secret/secret.yaml | kubeseal --format=yaml --cert=config/k8s/secret/sky-sealed-secrets.pem > config/k8s/secret/sealed/sealed-secret.yaml
   ```
6. Deploy sealed sky secrets:
   ```shell
   kubectl apply -f config/k8s/secret/sealed/sealed-secret.yaml
   ```
7. Create docker sealed credential form normal ones:
   Linux:
   ```shell
   kubeseal --format=yaml --cert=config/k8s/secret/sky-sealed-secrets.pem < config/k8s/secret/docker-cred.yaml > config/k8s/secret/sealed/sealed-docker-cred.yaml
   ```
   Windows:
   ```powershell
   Get-Content config/k8s/secret/docker-cred.yaml | kubeseal --format=yaml --cert=config/k8s/secret/sky-sealed-secrets.pem > config/k8s/secret/sealed/sealed-docker-cred.yaml
   ```
8. Deploy sealed docker repo credentials:
   ```shell
   kubectl apply -f config/k8s/secret/sealed/sealed-docker-cred.yaml
   ```

----------------------

### Kubernetes basic base64 encode secrets apply
```shell
kubectl apply -f config/k8s/secret.yaml
```
```shell
kubectl apply -f config/k8s/docker-cred.yaml
```
----------------------



## Logs
https://console.cloud.google.com/logs

parameters:
```shell
resource.type="k8s_container"
resource.labels.project_id="sky-app-17"
resource.labels.location="europe-central2"
resource.labels.cluster_name="sky-cluster"
resource.labels.namespace_name="default"
severity>=DEFAULT
```

----------------------

## Clearing

```shell
kubectl delete -f config/k8s/secret/secret.yaml
kubectl delete -f config/k8s/secret/sealed/sealed-docker-cred.yaml
kubectl delete -f config/k8s/secret/sealed/sealed-secret.yaml
kubectl delete -f config/k8s/secret/sealed-secrets-controller.yaml -n sealed-secrets
kubectl delete --from-file=./api-gateway/ingress/auth
kubectl delete -f config/k8s/api-gateway/ingress/ingress.yaml
kubectl delete -f config/k8s/api-gateway/oauth2-proxy/

kubectl delete -f config/k8s/db/mysql/
kubectl delete -f config/k8s/kafka/

kubectl delete -f config/k8s/service/sky-offer/
kubectl delete -f config/k8s/service/sky-booking/
kubectl delete -f config/k8s/service/sky-notify/
kubectl delete -f config/k8s/service/sky-message/
```

Simpler, you can run all scripts in a folder (in terminal being in parent folder):
```shell
kubectl delete -f config/k8s/secret/sealed --recursive
kubectl delete -f config/k8s/secret/sealed-secrets-controller.yaml -n sealed-secrets
kubectl delete -f config/k8s/api-gateway --recursive
kubectl delete -f config/k8s/db --recursive
kubectl delete -f config/k8s/kafka --recursive
kubectl delete -f config/k8s/service --recursive
```
