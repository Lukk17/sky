# syntax=docker/dockerfile:1.7

# Build from the repository root:
#   docker build -f apps/backend/sky-gateway/docker/Dockerfile -t sky-gateway:latest .

# Single shared pin: override with --build-arg RUNTIME_IMAGE=<image> to move all services at once.
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre-alpine
# ===============> BUILD STAGE <===============
FROM gradle:9-jdk25 AS build
WORKDIR /workspace
COPY apps/backend/settings.gradle.kts apps/backend/build.gradle.kts ./apps/backend/
COPY apps/backend/gradle ./apps/backend/gradle
COPY apps/backend/buildSrc ./apps/backend/buildSrc
COPY apps/backend/sky-common ./apps/backend/sky-common
COPY apps/backend/sky-gateway ./apps/backend/sky-gateway
WORKDIR /workspace/apps/backend
RUN --mount=type=cache,id=gradle-sky-gateway,target=/home/gradle/.gradle gradle :sky-gateway:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar sky-gateway/build/libs/sky-gateway.jar \
        extract --layers --launcher --destination sky-gateway/build/extracted

# ===============> RUN STAGE <===============
FROM $RUNTIME_IMAGE AS runtime

COPY config/keycloak/certs/localhost-ca.crt /tmp/local-dev-ca.crt
RUN keytool -importcert -noprompt \
      -alias local-dev-ca \
      -file /tmp/local-dev-ca.crt \
      -keystore "$JAVA_HOME/lib/security/cacerts" \
      -storepass changeit \
 && rm /tmp/local-dev-ca.crt

RUN addgroup -S -g 1000 sky && adduser -S -u 1000 sky -G sky
USER sky
WORKDIR /app
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-gateway/build/extracted/dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-gateway/build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-gateway/build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-gateway/build/extracted/application/ ./
EXPOSE 5777
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.net.preferIPv4Stack=true"
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:5777/actuator/health/liveness | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
