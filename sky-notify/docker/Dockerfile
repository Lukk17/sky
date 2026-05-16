# Build from repo root:
#   docker build -f sky-notify/docker/Dockerfile -t sky-notify:latest .

# ===============> BUILD STAGE <===============
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY sky-common ./sky-common
COPY sky-notify ./sky-notify
RUN gradle :sky-notify:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar sky-notify/build/libs/sky-notify.jar \
        extract --layers --launcher --destination sky-notify/build/extracted

# ===============> RUN STAGE <===============
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S sky && adduser -S sky -G sky
USER sky
WORKDIR /app
COPY --from=build --chown=sky:sky /workspace/sky-notify/build/extracted/dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/sky-notify/build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=sky:sky /workspace/sky-notify/build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/sky-notify/build/extracted/application/ ./
EXPOSE 5554
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:5554/actuator/health/liveness | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
