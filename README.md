#Sky  

 [![Build Status](https://travis-ci.org/Lukk17/sky.svg?branch=master)](https://travis-ci.org/Lukk17/sky)


Backend application for handling offers and booking them by users.  
Working via rest API. Example can be seen in [Sky-View](https://github.com/Lukk17/sky-view) application.

---------------------------------

| Microservice  | Description | Port |
| ------------- | ------------- | ------------- |
| eureka-service  | Naming server  | 8871 |
| common  | Service with common code  | 9200 |
| auth-service | Authorization service. Generate access tokens.  | 9100 |
| sky-user  | Service managing users. Like registering new ones.  | 5551 |
| sky-offer  | Service handling operation connected with offers.  | 5552 |
| sky-message | Service handling sending and receiving messages between users  | 5553 |
| zuul-service | Gateway service. Authenticate users and forward request to right microservices.  | 8872 |

---------------------------------

How it works
---------------------------------
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

Launch order:
---------------------------------
1. eureka-service
2. common
3. auth-service
4. sky-user
5. sky-offer
6. sky-message
7. zuul-service
---------------------------------

Required MySQL databases:
---------------------------------
``` 
"sky_user"
"sky_offer"
"sky_message" 
```

_Admin and roles must be added to DB_  
mysl_dumb can be used.

---------------------------------
Connecting with localhost MySQL
---------------------------------
MySQL can be run on localhost with port forwarding using e.g. [ngrok](https://ngrok.com/) 

`ngrok tcp 3306`

in output there is line about forwarding like that:  
`Forwarding                    tcp://0.tcp.ngrok.io:11236 -> localhost:3306 `

where  
`0.tcp.ngrok.io:11236` is address which should be used instead `localhost`

then in docker-compose.yml you need to change SPRING_DATASOURCE_URL to ngrok one: 
 
- by updating [.env file](#environment-external-config)
 

or  
- changing it in EVERY microservice if not using .env file

For example from:  
`SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/sky_offer?<restOfParametersHere>`  
To  
`SPRING_DATASOURCE_URL=jdbc:mysql://0.tcp.ngrok.io:11236/sky_offer?<restOfParametersHere>`
  
<br>
  
For alternative way to lunch DB see [this](#adding-mysql-server-to-docker) section.

---------------------------------

Build and Run
---------------------------------

To run build project with commend:

` mvn clean install -DskipTests`

---------------------------------
Run docker composer:

` sudo docker-compose up --build`

with clean build:

`sudo docker-compose build --no-cache `

---------------------------------

Useful
---------------------------------

#### Token example in header:  
`Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbkBhZG1pbiI...<restOfToken>`

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

#### Environment external config

External file with environmental config must be named ```.env``` 
and be in same directory as ```docker-compose.yml```

Stored values should represent key-value structure:

```yaml
NAMING_SERVICE_URL=http://eureka-service:8761/eureka
SPRING_PROFILES_ACTIVE=docker
MYSQL_PORT=3306
MYSQL_HOST=localhost
```
