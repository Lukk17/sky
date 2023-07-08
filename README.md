# Sky

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
- [Token example in a header](#token-example-in-a-header)
- [Environment external config](#environment-external-config)
- [Adding MySQL server to docker](#adding-mysql-server-to-docker)

---------------------------------

## How it works

Microservices are designed to work in Kubernetes cluster which will take care of user authentication and
authorization. Kubernetes Ingress controller is exposing services to the world.
Each microservice will have its API endpoints exposed.  

More info about [Ingres](./config/k8s/api-gateway/ingress/ingress_README.md).

For development services can be run as Spring Boot app or via Gradle.  
See more:
[Local development](./config/local-dev/local_README.md)


---------------------------------

## Required

### MySQL

`sky` which should be configured before lunching services.  
See [DB configuration](#DB-configuration) for manual how to configure.

### Kafka

For installation see [Local development](./config/local-dev/local_README.md).

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

# DB configuration

The Fastest way to configure the DB is:

1. create a schema named "sky".
2. start all sky services
3. run in sky DB: ./config/script/sql_commands/sql_offers_insert.sql
4. run in sky DB: ./config/script/sql_commands/sql_messages_insert.sql

---------------------------------

## Token example in a header

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbkBhZG1pbiI...<restOfToken>
```

## Environment external config

An external file with an environmental config are in ```.env``` file
which is in same directory as ```docker-compose.yml```

