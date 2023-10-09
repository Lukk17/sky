# Local running

### Part 1. Running on Minikube
- [Minikube setup](#minikube-setup)
- [Accessing app](#accessing-app)
- [Services deployment](#services-deployment)

### Part 2. Running with Gradle
- [Kafka install and run](#kafka-install-and-run)
- [Build and Run with Gradle](#build-and-run-with-gradle)

### Part 3. Running with Docker
- [Running app in Docker](#running-app-in-docker)
- [Adding MySQL server to docker](#adding-mysql-server-to-docker)

### Part 4. Extras
- [Troubleshooting](#troubleshooting)
- [Clearing](#clearing)

---------------------------------

## Minikube setup


### Start minikube with more resources
```shell
minikube start --cpus 4 --memory 16384
```
or with a different driver than default (docker)
```shell
minikube start --cpus 4 --memory 16384 --driver=virtualbox
minikube start --cpus 4 --memory 16384 --driver=docker
```
for linux only
```shell
minikube start --cpus 4 --memory 16384 --driver=kvm2
```
for windows only
```shell
minikube start --cpus 4 --memory 16384 --driver=hyperv
```
Driver can be checked in Minikube configuration in  
on Linux
```
~/.minikube/profiles/minikube/config.json
``` 
on Windows
```
%USERPROFILE%\.minikube\profiles\minikube\config.json
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

### Update minikube context:
After that, kubernetes know what it is working and know its config.
```shell
minikube update-context
```

### Get minikube IP
```shell
minikube ip
```

### minikube dashboard - terminal needs to remain open
```shell
minikube dashboard
```
or just url address :
```shell
minikube dashboard --url
```

-------------
## Accessing app

### URL
App should be accessible from URL:  
`http://<minikubeIP>/offer/api/home`

But there can be some problems because a docker driver adds a layer of networking,
which can complicate things and make the app not accessible.  
As a **workaround** to access cluster nginx use port forwarding:
```shell
kubectl port-forward svc/ingress-nginx-controller -n ingress-nginx 80:80 443:443
```
now app will be accessible and under:  
`http://localhost/offer/api/home`

### With host defined in ingress

Edit file (as admin):  
on Windows
```
C:\Windows\System32\drivers\etc\hosts
```
on Linux
```
/etc/hosts
```

adding:
```
127.0.0.1 skycloud.luksarna.com
```
where `skycloud.luksarna.com` is host name from ingress

then the app will respond at address:
```
http://skycloud.luksarna.com/offer/api/home
```  
where `offer/api/home` is service endpoint

-------------

## Services deployment


### Generate auth file with secrets (must be generated in api-gateway/ingress folder):
```shell
htpasswd -c auth <username>
```
where `username` will be user to log in via basic auth in nginx ingress   
it will prompt for password

### Run deployment script:
`services-deploy.sh`

mysql, sky-offer, sky-booking and sky-message services may require restarting due to creation of storage, etc.


---------------------------------

## Kafka install and run

1. Download binary from:  
   https://kafka.apache.org/downloads
2. Extract to place like `C:/kafka`
3. In `config` folder open `zookeeper.properties`
   set field `dataDir` with a path like `C:/kafka/zookeeper-data`
4. In `config` folder open `server.properties`
   set field `log.dirs` with a path like `C:/kafka/kafka-logs`
5. Open the terminal in kafka directory and use command:
   ```shell
      .\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties
   ```
   or use full path like:
   ```shell
   D:\Development\kafka\bin\windows\zookeeper-server-start.bat D:\Development\kafka\config\zookeeper.properties
   ```
   <br>
6. In another terminal in kafka directory use command:
   ```shell
      .\bin\windows\kafka-server-start.bat .\config\server.properties
   ```
   or use full path like:
   ```shell
   D:\Development\kafka\bin\windows\kafka-server-start.bat D:\Development\kafka\config\server.properties
   ```
---------------------------------

## Build and Run with Gradle

### All run configurations are saved in the folder:
```
.idea\runConfigurations
```

To run a build project with command:
```
gradle clean bootRun --args='--spring.profiles.active=local'
```

It will be using default environment variables.  
To change them, add all variables to an operating system.

---------------------------------

## Running app in Docker

It can be run by docker-compose file or individually via Dockerfiles.

#### Remember of adding env variable to your system or use Intellij RunConfiguration which has those variables.

### A) Using [docker-compose.yml](../docker/docker-compose.yml)

After starting, give containers a minute or so to fully start and connect with each other.  
Before that, there could be 500 errors.  
This log need to appear in all containers:  
`Getting all instance registry info from the eureka server`


In the main project folder (before any modules) run:
```
docker-compose -f config/docker/docker-compose.yml up
```  

or in "config/docker/" folder:
```
docker-compose up
```  

or if you want to rebuild all:
```
docker-compose -f config/docker/docker-compose.yml up --build
```

or with clean build:
```
docker-compose -f config/docker/docker-compose.yml build --no-cache
```

### B) Using Dockerfiles, create and start/run methods

#### Prerequisite

Create a network for microservices:
```
docker network create sky-net
```

ALL DOCKER BUILD COMMANDS NEED TO BE STARTED FROM the MAIN (SKY) FOLDER,   
NOT FROM EACH MODULE FOLDER  
due to the gradle build dependency on config module Gradle file


#### 1. sky-booking

Build:
```
docker build . -f sky-booking/docker/Dockerfile -t sky-booking:latest --no-cache
```  

Docker container creation:
```
docker create --name sky-booking --network sky-net sky-booking:latest
```  

Starting a container:
```
docker start sky-booking
```

#### 2. sky-offer

Build:
```
docker build . -f sky-offer/docker/Dockerfile -t sky-offer:latest --no-cache
```  

Docker container creation:
```
docker create --name sky-offer --network sky-net sky-offer:latest
```  

Starting a container:
```
docker start sky-offer
```

#### 3. sky-notify

Build:
```
docker build . -f sky-notify/docker/Dockerfile -t sky-notify:latest --no-cache
```  

Docker container creation:
```
docker create --name sky-notify --network sky-net sky-notify:latest
```  

Starting a container:
```
docker start sky-notify
```

#### 4. sky-message

Build:
```
docker build . -f sky-message/docker/Dockerfile -t sky-message:latest --no-cache
```  

Docker container creation:
```
docker create --name sky-message --network sky-net sky-message:latest
```  

Starting a container:
```
docker start sky-message
```  


#### Running instead creating containers:
```
docker run sky-booking:latest 
docker run sky-offer:latest 
docker run sky-notify:latest 
docker run sky-message:latest 
```  

Now you need to add them into the same network:
```  
docker network connect sky-net sky-booking  
docker network connect sky-net sky-offer  
docker network connect sky-net sky-notify  
docker network connect sky-net sky-message 
```

If the network is not needed can be removed with:
```
docker network rm sky-net
```

---------------------------------

## Adding MySQL server to docker

For every microservice that needs its one database MySQL DB image should be created in docker.   
Mysql image can be added to docker-compose.yml, for example, sky-offer DB image should look like:

```yaml
  mysql-sky_offer:
    image: 'mysql:latest'
    restart: always
    environment:
      - MYSQL_ROOT_PASSWORD=XXX
      - MYSQL_DATABASE=sky_offer
    ports:
      - 3307:3306
    expose:
      - 3306
```

In microservice docker-compose.yml description dependency to right MySQL image needs to be added:

```yaml
    depends_on:
      - mysql-sky_offer
```


---------------------------------

## Troubleshooting

### Load Balancer
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

### Minikube

#### Changing Windows Docker max RAM usage

Create `.wslconfig` in user directory
populate with this config:
```
[wsl2]
memory=26GB # Limits VM memory in WSL 2 to 4 GB
processors=6 # Makes the WSL 2 VM use two virtual processors
```
restart service `LxssManager` to restart WSL settings

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

#### If not working, start in containerd runtime:

```shell
minikube start --container-runtime=containerd
```

or via setting parameter:
```shell
minikube config set container-runtime containerd
```
Valid options: docker, cri-o, containerd (default: auto)

Sometimes change of a driver helps:
```shell
minikube start --driver=none
```

#### If not working, try cache images:
```shell
minikube cache add <dockerhub username>/<repo name>:<version optional>
```

due to cache deprecation best to use:
```shell
minikube image load <dockerhub username>/<repo name>:<version optional>
```

AND add in deployment's definition:
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

#### minikube visibility under localhost (127.0.0.1") - terminal needs to remain open
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


-------------


## Clearing

```shell
minikube stop
```

```shell
minikube delete
```

full delete:
```shell
minikube delete --all
```
