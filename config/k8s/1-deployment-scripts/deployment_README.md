# DEPLOYMENT

----------------------

## GCP

1. Install gcloud CLI  
https://cloud.google.com/sdk/gcloud?authuser=1  
and plugin:
    ```shell
    sudo apt-get install google-cloud-sdk-gke-gcloud-auth-plugin
    ```

2. Set project
    ```shell
    gcloud config set project sky-app-17
    ```

3. Create cluster
    ```shell
    gcloud container clusters create-auto sky-cluster --location=europe-central2
    ```

4. Get authentication credentials for the cluster - required to interact with cluster
   ```shell
   gcloud container clusters get-credentials sky-cluster --location=europe-central2
   ```

5. Install nginx ingress
   ```shell
   helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
   helm repo update
   helm install ingress-nginx ingress-nginx/ingress-nginx  
   ```

----------------------

## Auth service
1. Creating project in auth0 for authentication  
   https://manage.auth0.com/dashboard


2. Changing clientId, clientSecret and domain in oauth2 deployment config  
   https://kubernetes.github.io/ingress-nginx/examples/auth/oauth-external-auth/

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

----------------------

## After deployment on GCP:
https://console.cloud.google.com/kubernetes/discovery?

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
nginx: 34.118.116.39:80

