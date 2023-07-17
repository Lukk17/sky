# Helm

## Table of content

- [Pre-requisitions](#1-pre-requisitions)
- [Api gateway](#2-api-gateway)
- [Independent services](#3-independent-services)
- [Database](#4-database)
- [Install service charts](#5-install-service-charts)
- [Changing application address](#6-changing-application-address)
- [Clearing](#7-clearing)
- [Troubleshooter](#8-troubleshooter)
- [Commands](#9-commands)

---------

## 1. Pre-requisitions

### Make sure you are using correct context (project)

```shell
kubectl config use-context my-cluster-name
```

can be checked by
```shell
kubectl config current-context
```

---------

### Secrets

#### Install Sealed Secrets
1. Create namespace
    ```shell
    kubectl create namespace sealed-secrets
    ```
2. Create a TLS secret from public.crt and private.key  
   These keys are not stored in repo - should be stored in protected location like password manager.  
   See [this](../_deployment-scripts/deployment_README.md#create-private-and-public-keys-for-sealed-secrets)
   for creating new keys.

    ```shell
   kubectl create secret tls sealed-secrets-key --cert=./config/k8s/secret/sealed-public.crt --key=./config/k8s/secret/sealed-private.key -n sealed-secrets
   ```
3. Install sealed secrets controller  
   Without `fullnameOverride` controller will have name `sealed-secrets-controller-sealed-secrets`  
   because namespace will be added as suffix (`-sealed-secrets`)
   `--set generatePrivateKey=false` will not generated private key but used one generated before
    <br> <br>
   1. Local version (pulled v0.22.0)
      
       ```shell
       helm install sealed-secrets-controller ./config/k8s/helm/sealed-secrets-controller/ -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
       ```
      
   2. Install latest version
       ```shell
       helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
       helm repo update
       helm install sealed-secrets-controller sealed-secrets/sealed-secrets -n sealed-secrets --set generatePrivateKey=false --set fullnameOverride=sealed-secrets-controller
       ``` 
   
4. Deploy sealed secrets (should be in already in repo)
   ```shell
     kubectl apply -f config/k8s/secret/sealed/sealed-secrets.yaml
     kubectl apply -f config/k8s/secret/sealed/sealed-docker-cred.yaml
   ```
   To create new credentials or update with new see [this](../_deployment-scripts/deployment_README.md#create-new-sealed-secrets).


---------

## 2. Api gateway

### oAuth-proxy

Version 7.4.0  
```shell
  helm install oauth2-proxy ./config/k8s/helm/oauth2-proxy/
 ```

---------

## 3. Independent services

### Kafka
1. Local version (pulled v3.5.0)
    ```shell
    helm install kafka ./config/k8s/helm/kafka/
    ```

2. Install latest version
    ```shell
    helm install kafka oci://registry-1.docker.io/bitnamicharts/kafka
    ```

---------

## 4. Database

### MySQL

Version (pulled v8.0.33)
```shell
helm install mysql ./config/k8s/helm/mysql/
```

---------

## 5. Install service charts

```shell
helm install sky-offer ./sky-offer
helm install sky-booking ./sky-booking
helm install sky-message ./sky-message
helm install sky-notify ./sky-notify
```

----------------------

## 6. Changing application address

When changing app web address (host) for example from  
`https://sky.luksarna.com` to `https://skycloud.luksarna.com`

1. In app config you need to change:
   * Every Ingress `spec.rules.host` in each service `values.yaml`
   * Every Ingress `nginx.ingress.kubernetes.io/auth-url` and `nginx.ingress.kubernetes.io/auth-signin`
   * `redirect-url` in `oauth2-proxy` `values.yaml`  
     <br>
2. Update oAuth2 providers:  
   <br>
   * Auth0: https://manage.auth0.com/dashboard  
     under Application -> Application -> sky (app name)
     put new address in field `Allowed Callback URLs`.
     Addresses are separated by comma `,` and can be inserted in new line  
     <br>
   * Google: https://console.cloud.google.com/apis/credentials  
     just add new address to selected oauth client `Authorized JavaScript origins` field.  
     <br>
   * Github: https://github.com/settings/developers  
     need to create new oauth app with new address as `Homepage URL`  
    <br>
3. Update Postman envs

--------

## 7. Clearing

```shell
helm uninstall sealed-secrets-controller -n sealed-secrets
kubectl delete secret sealed-secrets-key -n sealed-secrets
kubectl delete -f config/k8s/secret/sealed --recursive

helm uninstall sky-offer
helm uninstall sky-booking ./sky-booking
helm uninstall sky-message ./sky-message
helm uninstall sky-notify ./sky-notify
```

--------

## 8. Troubleshooter

### Error creating container

```
Failed to load logs: container "sky-offer-container" in pod "sky-offer-deployment-<containerHash>" is waiting to start: CreateContainerConfigError
```

Probably secrets are not created 
or sealed secrets controller where reinstalled and new cert needed to be fetched and all secrets re-encrypted.
See [this](../_deployment-scripts/deployment_README.md#create-new-sealed-secrets).


--------

### Wrong env values

```
INSTALLATION FAILED: 1 error occurred:
        * Deployment in version "v1" cannot be handled as a Deployment: json: cannot unmarshal number into Go struct field EnvVar.spec.template.spec.containers.env.value of type string
```
Solution: In helm `values.yaml` and `templates\deployment.yaml` every env value need to be quoted.

--------


```
Error: INSTALLATION FAILED: cannot re-use a name that is still in use
```

Already deployed with this name.  
Solution:  
uninstall deployment:
```shell
helm uninstall <deployment name>
```
example:
```shell
helm uninstall sky-offer
```

or use `helm upgrade` instead.

--------

## 9. Commands

### Install charts

```shell
helm install <release name> ./<base chart name - folder name>
```
example:
```shell
helm install sky-offer ./config/k8s/helm/sky-offer
```

--------

### Download chart (.tgz file)

```shell
helm pull <chart name>
```

--------

### Upgrade chart

```shell
helm upgrade <release name> ./<base chart name - folder name>
```
example:
```shell
helm upgrade sky-offer ./config/k8s/helm/sky-offer
```

--------

### List all installed charts

```shell
helm list
```
