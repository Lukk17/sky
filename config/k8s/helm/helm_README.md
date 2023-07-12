# Helm

---------

## Pre-requisitions

### Secrets

#### Install Sealed Secrets
1. Create namespace
    ```shell
    kubectl create namespace sealed-secrets
    ```
2. Install sealed secrets controller  
   Without `fullnameOverride` controller will have name `sealed-secrets-controller-sealed-secrets`  
   because namespace will be added as suffix (`-sealed-secrets`)
    <br> <br>
   1. Local version (pulled v0.22.0)
      
       ```shell
       helm install sealed-secrets-controller ./config/k8s/helm/sealed-secrets/ -n sealed-secrets --set fullnameOverride=sealed-secrets-controller
       ```
      
   2. Pull latest version
       ```shell
       helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
       helm repo update
       helm install sealed-secrets-controller sealed-secrets/sealed-secrets -n sealed-secrets --set fullnameOverride=sealed-secrets-controller
       ```
3. If installing first time or namespace was deleted or changes then new private and public cert will be generated when installing.
    You will need to encrypt secrets.  
   To create new credentials or update with new  see [this](../_deployment-scripts/deployment_README.md#create-new-sealed-secrets). 

---------

### Make sure you are using correct context (project)

```shell
kubectl config use-context my-cluster-name
```

can be checked by
```shell
kubectl config current-context
```

---------

## Install chart

```shell
helm install <release name> ./<base chart name - folder name>
```
example:
```shell
helm install sky-offer ./sky-offer
```

--------


### Upgrade chart

```shell
helm upgrade <release name> ./<base chart name - folder name>
```
example:
```shell
helm upgrade sky-offer ./sky-offer
```

--------

## List all installed charts

```shell
helm list
```

--------

## Clearing

```shell
helm uninstall sky-offer
helm uninstall sealed-secrets-controller -n sealed-secrets
```

--------

## Troubleshooter

### Error creating container

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
