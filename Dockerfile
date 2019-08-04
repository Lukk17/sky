FROM maven:3.5.2-jdk-8-alpine
EXPOSE 5555
WORKDIR /app
COPY ./ ./
RUN mvn -f ./pom.xml clean install
COPY ./target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
