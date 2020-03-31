# for mysql checking:
#FROM dostiharise/ubuntu-java-mysql

# ===============> BUILD STAGE <===============
FROM maven:3.5.2-jdk-8-alpine AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -e -f /home/app/pom.xml clean install


# ===============> RUN STAGE <===============
FROM openjdk:8-jdk-slim
COPY --from=build /home/app/target/*.jar app.jar
CMD ["java","-jar","app.jar"]
