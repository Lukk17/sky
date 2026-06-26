# Build from repo root:
#   docker build -f sky-gateway/docker/Dockerfile -t sky-gateway:latest .

# ===============> BUILD STAGE <===============
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY sky-gateway ./sky-gateway
RUN --mount=type=cache,target=/home/gradle/.gradle gradle :sky-gateway:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar sky-gateway/build/libs/sky-gateway.jar \
        extract --layers --launcher --destination sky-gateway/build/extracted

# ===============> RUN STAGE <===============
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S -g 1000 sky && adduser -S -u 1000 sky -G sky
USER sky
WORKDIR /app
COPY --from=build --chown=sky:sky /workspace/sky-gateway/build/extracted/dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/sky-gateway/build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=sky:sky /workspace/sky-gateway/build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/sky-gateway/build/extracted/application/ ./
EXPOSE 5777
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:5777/actuator/health/liveness | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
