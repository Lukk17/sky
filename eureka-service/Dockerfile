# for mysql checking:
#FROM dostiharise/ubuntu-java-mysql

# ===============> BUILD STAGE <===============
FROM maven:3.6.3-jdk-11 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -e -f /home/app/pom.xml clean install


# ===============> RUN STAGE <===============
FROM openjdk:11
COPY --from=build /home/app/target/*.jar app.jar
CMD ["java","-jar","app.jar"]
