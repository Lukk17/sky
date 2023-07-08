# DEPLOYMENT

----------------------

## Docker build and publish

Build:
```
docker build . -f sky-booking/docker/Dockerfile -t sky-booking:latest --no-cache
docker build . -f sky-offer/docker/Dockerfile -t sky-offer:latest --no-cache
docker build . -f sky-notify/docker/Dockerfile -t sky-notify:latest --no-cache
docker build . -f sky-message/docker/Dockerfile -t sky-message:latest --no-cache
```  

Push into repository:
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
```shell
kubectl apply -f ./secret.yaml
kubectl apply -f ./mysql/
kubectl apply -f ./sky-offer/
kubectl apply -f ./sky-message/
kubectl apply -f ./api-gateway/ingress/ingress.yaml
kubectl create secret generic basic-auth --from-file=./api-gateway/ingress/auth

kubectl apply -f ./api-gateway/oauth2-proxy/

```
Simpler, you can run all scripts in a folder (in terminal being in parent folder):
```shell
kubectl apply -f config/k8s/api-gateway --recursive
kubectl apply -f config/k8s/service --recursive
kubectl apply -f config/k8s/db --recursive
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
