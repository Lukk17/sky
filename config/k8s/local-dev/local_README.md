-------------
## Minikube setup

<br>

### Start minikube with more resources
```shell
minikube start --cpus 4 --memory 16384
```
on Windows, it needs setup in wsl - by creating `.wslconfig` file in home directory:
```
[wsl2]
memory=20GB   # Limits VM memory in WSL 2 up to 3GB
processors=4 # Makes the WSL 2 VM use two virtual processors
```

or set parameters before:
```shell
minikube config set memory 12288
minikube config set cpus 4
minikube config set disk-size 15000
```

### Adding nginx ingress addons:

list of addons:
```shell
minikube addons list
```

enabling addons:
```shell
minikube addons enable ingress
minikube addons enable ingress-dns
```

<br>

### Update minikube context:
After that kubernetes know on what it is working and know its config.
```shell
minikube update-context
```

<br> 

### Get minikube IP
```shell
minikube ip
```

<br>

### minikube dashboard - terminal need to remain open
```shell
minikube dashboard
```
or just url address :
```shell
minikube dashboard --url
```

<br>

-------------

## Services deployment

<br>

### Generate auth file with secrets (must be generated in api-gateway/ingress folder):
```shell
htpasswd -c auth <username>
```
where `username` will be user to log in via basic auth in nginx ingress  
it will prompt for password

### Run deployment script:
`services-deploy.sh`

mysql, sky-offer and sky-message services may require restarting due to creation of storage etc.

<br>

-------------
## Accessing app

App should be accessible from URL:
`http://<minikubeIP>/offer`

if type loadBalancer after minikube tunnel
`http://<minikubeIP>:<external port>/`
to get external port run:
```shell
kubectl get service sky-offer-service        

```
you will get something like that
```shell
NAME                TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)          AGE
sky-offer-service   LoadBalancer   10.106.230.5   10.106.230.5   5552:31182/TCP   5h43m
```
under ports there is `5552:31182/TCP` - you need to use `31182` port.

-------------

## Accessing cluster on local the machine (no Load balancer configured)

<br>

There are two ways
1. `minikube tunnel`  
   it will forward traffic to ingresses


2. MetalLB install  - problem with configuring it  
   Use yaml files or install via url (can be found in `installingMetalLB.sh`)
   At beginning, it can fail due to lack of "memberlist" secret, but it will start working in minute

<br>

## Troubleshooting

### Minikube

#### If error with pulling docker images:

pull images with this command:
```shell
minikube ssh docker pull <imageName>
```
examples:
```shell
minikube ssh docker pull mysql
minikube ssh docker pull quay.io/keycloak/keycloak:19.0.3
minikube ssh docker pull lukk17/sky-offer
minikube ssh docker pull lukk17/sky-message
```

<br>

#### If not working start in containerd runtime:

```shell
minikube start --container-runtime=containerd
```

or via setting parameter:
```shell
minikube config set container-runtime containerd
```
Valid options: docker, cri-o, containerd (default: auto)

Sometimes change of driver helps:
```shell
minikube start --driver=none
```

<br>

#### If not working try cache images:
```shell
minikube cache add <dockerhub username>/<repo name>:<version optional>
```

due to cache deprecation best to use:
```shell
minikube image load <dockerhub username>/<repo name>:<version optional>
```

AND add in deployments definition:
```yaml
spec:
    template:
        spec:
            containers:
                imagePullPolicy: Never
```

examples:
```shell
minikube image load mysql
minikube image load quay.io/keycloak/keycloak:19.0.3
minikube image load lukk17/sky-offer
minikube image load lukk17/sky-message
```

additional thing to try:
```shell
docker pull <imageName>
```
<br>

#### minikube visibility under localhost (127.0.0.1") - terminal need to remain open
```shell
minikube tunnel
```
or else get its address:
```shell
minikube ip
```

create a tunnel for service:
```shell
minikube service -n default <serviceName> --url
```
where `serviceName` is from `kubectl get service`
and `default` is namespace

<br>


-------------


<br>

### Clearing

```shell
minikube delete
```
