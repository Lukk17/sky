# need to be run from top folder, not from inside module !
ARG APP_NAME=eureka-service
ARG HOME=/home/app

# ===============> BUILD STAGE <===============
FROM gradle:7.4.2-jdk11 AS build
ARG APP_NAME
ARG HOME
LABEL stage=build
COPY ./$APP_NAME/ $HOME/
COPY ./config/*.gradle.kts /home/config/
RUN gradle -p $HOME clean bootJar

# ===============> RUN STAGE <===============
FROM openjdk:11
ARG APP_NAME
# only ENV is available in runtime, ARG is only avalaible in build
ENV APP_NAME $APP_NAME
ARG HOME
COPY --from=build $HOME/build/libs/$APP_NAME.jar ./$APP_NAME.jar
EXPOSE 8761
ENTRYPOINT java -jar $APP_NAME".jar"
