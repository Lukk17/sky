# Build from repo root:
#   docker build -f sky-offer/docker/Dockerfile -t sky-offer:latest .

# ===============> BUILD STAGE <===============
FROM gradle:8.14.3-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY sky-common ./sky-common
COPY sky-offer ./sky-offer
RUN gradle :sky-offer:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar sky-offer/build/libs/sky-offer.jar \
        extract --layers --launcher --destination sky-offer/build/extracted

# ===============> RUN STAGE <===============
FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S sky && adduser -S sky -G sky
USER sky
WORKDIR /app
COPY --from=build --chown=sky:sky /workspace/sky-offer/build/extracted/dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/sky-offer/build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=sky:sky /workspace/sky-offer/build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/sky-offer/build/extracted/application/ ./
EXPOSE 5552
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:5552/actuator/health/liveness | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
