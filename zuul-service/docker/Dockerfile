# need to be run from top folder, not from inside module !
ARG APP_NAME=zuul-service
ARG HOME=/home/app

# ===============> BUILD STAGE <===============
FROM gradle:7.4.2-jdk11 AS build
ARG APP_NAME
ARG HOME
LABEL stage=build
COPY ./$APP_NAME/ $HOME
COPY ./config/*.gradle /home/config/
RUN gradle -p $HOME bootJar


# ===============> RUN STAGE <===============
FROM openjdk:11
ARG HOME
COPY --from=build $HOME/build/libs/*.jar app.jar
EXPOSE 8762
CMD ["java","-jar","app.jar"]
