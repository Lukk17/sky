#Sky  

 [![Build Status](https://travis-ci.org/Lukk17/sky.svg?branch=master)](https://travis-ci.org/Lukk17/sky)


Backend application for handling offers and booking them by users.  
Working via rest API. Example can be seen in [Sky-View](https://github.com/Lukk17/sky-view) application.


| Microservice  | Description | Port |
| ------------- | ------------- | ------------- |
| eureka-service  | Naming server  | 8871 |
| auth-service | Authorization service. Generate access tokens.  | 9100 |
| sky-offer  | Service handling operation connected with offers.  | 5552 |
| sky-message | Service handling sending and receiving messages between users  | 5553 |
| zuul-service | Gateway service. Authenticate users and forward request to right microservices.  | 8872 |


#Table of content
- [How it works](#How-it-works)
- [Launch order](#Launch-order)
- [Required](#required)
- [Build and Run](#Build-and-Run)
- [Running app in Docker](#Running-app-in-Docker)
- [Token example in a header](#Token-example-in-a-header)
- [Environment external config](#Environment-external-config)
- [DB configuration](#DB-configuration)
- [Adding MySQL server to docker](#Adding-MySQL-server-to-docker)


---------------------------------
# How it works

Zuul service, which is gateway catches all request, check if user is authenticated - by checking [token](#token-example-in-header) send in request header.
If user is not authenticated then it can access only few not secured endpoints.  
Next Zuul ask naming server (eureka-server) for microservice address and then forward request to it.

All request should go through gateway.  
For example if you want to get all offers you should send request for address:
`http://localhost:8762/offer/getAll`

where `localhost:8762` is gateway (Zuul) address.

Do NOT send request directly to microservice like:  
`http://localhost:5552/getAll`

---------------------------------

# Launch order:

1. eureka-service
2. auth-service
3. sky-user
4. sky-offer
5. sky-message
6. zuul-service
---------------------------------

# Required 
MySQL database:  
`sky` which should be configured before lunching services.

---------------------------------

# Build and Run


To run build project with commend:

` mvn clean install -DskipTests`

---------------------------------

# Running app in Docker

It can be run by docker-compose file or individually via Dockerfiles.

### A) Using docker-compose.yml

After starting give containers minute or so to fully start and connect with each others.  
Before that, there could be 500 errors.  
This log need to appear in all containers:  
`Getting all instance registry info from the eureka server`


In main project folder (before any modules) run:  
`docker-compose -f config/docker/docker-compose.yml up`  

or in "config/docker/" folder:  
`docker-compose up`  

or if you want to rebuild all:  
`docker-compose -f config/docker/docker-compose.yml up --build`

or with clean build:  
`docker-compose -f config/docker/docker-compose.yml build --no-cache`

### B) Using Dockerfiles, creates and start/run methods  

#### Prerequisite
Create a network for microservices:  
`docker network create sky-net`

#### 1. eureka-service
This one need to have port published.  
Build:  
`docker build . -t eureka-service:latest`   
Docker container creation:  
`docker create --name eureka-service --network sky-net --publish 8761:8761 eureka-service:latest`  
Starting a container:  
`docker start eureka-service`  


#### 2. auth-service
Build:  
`docker build . -t auth-service:latest`  
Docker container creation:  
`docker create --name auth-service --network sky-net auth-service:latest`  
Starting a container:  
`docker start auth-service`

#### 3. sky-offer
Build:  
`docker build . -t sky-offer:latest`  
Docker container creation:  
`docker create --name sky-offer --network sky-net sky-offer:latest`  
Starting a container:  
`docker start sky-offer`

#### 4. sky-message
Build:  
`docker build . -t sky-message:latest`  
Docker container creation:  
`docker create --name sky-message --network sky-net sky-message:latest`  
Starting a container:  
`docker start sky-message`  

#### 5. zuul-service
Build:  
`docker build . -t zuul-service:latest`  
Docker container creation:  
`docker create --name zuul-service --network sky-net --publish 8762:8762 zuul-service:latest`  
Starting a container:  
`docker start zuul-service`  


#### Running instead creating containers:
```
docker run -p 8761:8761 eureka-service:latest 
docker run auth-service:latest 
docker run sky-offer:latest 
docker run sky-message:latest  
docker run -p 8762:8762 zuul-service:latest
```  

Now you need to add them into same network:  
``` docker network connect sky-net eureka-service  
docker network connect sky-net auth-service  
docker network connect sky-net sky-offer  
docker network connect sky-net sky-message 
docker network connect sky-net zuul-service 
```


---------------------------------
Useful
---------------------------------

#### Token example in a header:  
`Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbkBhZG1pbiI...<restOfToken>`

---------------------------------
#### Environment external config

External file with environmental config are in ```.env``` file
which is in same directory as ```docker-compose.yml```


---------------------------------
#### DB configuration

This db needs to be created before running db dump file.

_Admin and roles must be added to DB_  
mysl_dumb can be used. (basicDump do not have offer, message and booked table - which skyFullDump have)

##### When using sql-dump admin password will not be correctly encrypted.
To repair it register new user with same password as admins (below) and copy it as admin password directly in user DB.

admin user credentials from mysql_dumb:
```
{
	"username": "admin@admin",
	"password": "admin"
}
```

### What needs to be done within DB (when no using mysql dump file)

Admin role should be named "ROLE_ADMIN"
Admin role should have id 0

User role should be named "ROLE_USER"
User role should have id 1

put it into SQL DB:
``` 
INSERT INTO sky_user.role VALUES (0, 'ROLE_ADMIN');
INSERT INTO sky_user.role VALUES (1, 'ROLE_USER');
``` 

Create admin and test users through API endpoint:
localhost:8762/register
with json payload:
``` 
{
"email": "admin@admin",
"password": "admin"
}
```
and
``` 
{
"email": "test@test",
"password": "test"
}
``` 

Then change admin user role to ROLE_ADMIN (role_id=0):
``` 
UPDATE sky_user.user_role SET role_id = 0 WHERE user_id = {id};
``` 
where {id} is ID of admin user. You can check it with:
``` 
SELECT id FROM sky_user.user WHERE email='admin@admin';
``` 

If something gone wrong you can delete admin user and re-register it:
``` 
DELETE FROM sky_user.user_role WHERE user_id = {id};
DELETE FROM sky_user.user WHERE id = {id};
``` 
where {id} is ID of admin user. It needs to be deleted from user_role table first.

##### After all this now you can put into the DB some offers.

---------------------------------

#### Adding MySQL server to docker
For every microservice which needs its one database MySQL DB image should be created in docker.   
Mysql image can be added to docker-compose.yml for example sky-offer DB image should looks like:
```yaml
  mysql-sky_offer:
    image: 'mysql:latest'
    restart: always
    environment:
      - MYSQL_ROOT_PASSWORD=Lukk1234
      - MYSQL_DATABASE=sky_offer
    ports:
      - 3307:3306
    expose:
      - 3306
```

In microservice docker-compose.yml description dependency to right MySQL image need to be added:
```yaml
    depends_on:
      - eureka-service
      - mysql-sky_offer
```

