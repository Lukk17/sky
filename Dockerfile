FROM openjdk:8-jdk-alpine
VOLUME /tmp
EXPOSE 5555
ADD target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
