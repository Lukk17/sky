# Sky

App deployed in Kubernetes on GCP:

https://skycloud.luksarna.com/

---------------------------------


Backend application for handling offers and booking them by users.  
Working via rest API. Example can be seen in [Sky-View](https://github.com/Lukk17/sky-view) application.

| Microservice | Description                                                   | Port | README                             |
|--------------|---------------------------------------------------------------|------|------------------------------------|
| sky-booking  | Service handling operation connected with offers bookings.    | 5555 | [Booking](./sky-booking/README.md) |
| sky-offer    | Service handling operation connected with offers.             | 5552 | [Offer](./sky-offer/README.md)     |
| sky-message  | Service handling sending and receiving messages between users | 5553 | [Message](./sky-message/README.md) |
| sky-notify   | Service handling notification propagation to client           | 5554 | [Notify](./sky-notify/README.md)   |

## Table of content

- [How it works](#how-it-works)
- [Required](#required)
- [Environment variable](#environment-variable)
- [Docker build and publish](#docker-build-and-publish)
- [Kubernetes deployment](#kubernetes-deployment)
- [DB configuration](#db-configuration)

---------------------------------

## How it works

Microservices are designed to work in Kubernetes cluster which will take care of user authentication and
authorization. Kubernetes Ingress controller is exposing services to the world.
Each microservice will have its API endpoints exposed.  

### Ingress
More info about [Ingres](./config/k8s/vanilla/api-gateway/ingress/ingress_README.md).

### Postman
In [postman-collection](./config/postman-collection). 
Folder you can find an exported sky collection and envs.

### Dev
For development services can be run as Spring Boot app or via Gradle.  
See more:
[Local development](./config/local-dev/local_README.md)

### Swagger
Swagger is added and can be accessed under:  
`<app address>/swagger-ui/index.html`  
example:  
`http://localhost:5552/swagger-ui/index.html`

For Kubernetes Swagger access, see [this](./config/k8s/k8s_README.md#swagger-access)

---------------------------------

## Required

### MySQL

`sky` which should be configured before lunching services.  
See [DB configuration](#DB-configuration) for manual how to configure.

### Kafka

* Kubernetes deployment: [Kafka kubernetes](./config/k8s/vanilla/kafka/kafka_README.md).  
* Local installation: [Local development](./config/local-dev/local_README.md).

---------------------------------

## Environment variable

List all used variables with default values (in docker):

```
SPRING_PROFILES_ACTIVE = default;
MYSQL_HOST = host.docker.internal;
MYSQL_PORT = 3306;
DB_PARAMS = useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=Europe/Warsaw;
MYSQL_USER = YYY;
MYSQL_PASS = XXX;
MYSQL_DATABASE_NAME = sky;
SPRING_DEBUG = INFO;
HIBERNATE_DEBUG = INFO;
MYSQL_DATABASE_NAME = sky;
SHOW_SQL_QUERIES = false;
SPRING_SECURITY_USER = XYZ;
SPRING_SECURITY_PASS = XYZ;
```

---------------------------------

## Docker build and publish

See [Deployment](./config/k8s/_deployment-scripts/deployment_README.md) for more info.


---------------------------------

## Kubernetes deployment

See  [Deployment](./config/k8s/k8s_README.md) for more info.

---------------------------------

## DB configuration

The Fastest way to configure the DB is:

1. create a schema named "sky".
2. start all sky services
3. run in sky DB: ./config/script/sql_commands/sql_offers_insert.sql
4. run in sky DB: ./config/script/sql_commands/sql_messages_insert.sql

---------------------------------
