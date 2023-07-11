# Helm

---------

## Pre-requisitions

### secrets

Secrets needs to be created before starting helm charts, because they are using them.
It is not good practice to store secrets in helm itself.

```shell
kubectl apply -f config/k8s/secret.yaml
```
where secrets should look like:
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


```shell
kubectl apply -f config/k8s/docker-cred.yaml
```
where docker-cred should look like:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: docker-cred
type: kubernetes.io/dockerconfigjson
stringData:
  docker-server: <your-registry-server>
  docker-username: <your-name>
  docker-password: <your-password>

```

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

### Install chart

```shell
helm install <release name> ./<base chart name - folder name>
```

```shell
helm install sky-offer ./sky-offer
```


--------

## Troubleshooter

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

--------
