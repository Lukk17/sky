FROM openjdk:8-jdk-alpine
VOLUME /tmp
EXPOSE 5555
CMD["mvn", "clean", "install" ]
ADD target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
