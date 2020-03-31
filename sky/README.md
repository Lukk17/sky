# Sky 
[![Build Status](https://travis-ci.org/Lukk17/sky.svg?branch=master)](https://travis-ci.org/Lukk17/sky)

<br>

#### This project uses docker.

It needs MySQL database named "sky"

_Admin and roles must be added to DB_

---------------------------------
<br>

To run build project with commend:

` mvn clean install -DskipTests`

then run docker composer:

` sudo docker-compose up --build`

clean build:

`sudo docker-compose build --no-cache `

