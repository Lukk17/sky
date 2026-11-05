# syntax=docker/dockerfile:1.7

# Build from the repository root:
#   docker build -f apps/backend/sky-offer/docker/Dockerfile -t sky-offer:latest .

# Single shared pin: override with --build-arg RUNTIME_IMAGE=<image> to move all services at once.
ARG RUNTIME_IMAGE=eclipse-temurin:25-jre-alpine
# ===============> BUILD STAGE <===============
FROM gradle:9-jdk25 AS build
WORKDIR /workspace
COPY apps/backend/settings.gradle.kts apps/backend/build.gradle.kts ./apps/backend/
COPY apps/backend/gradle ./apps/backend/gradle
COPY apps/backend/buildSrc ./apps/backend/buildSrc
COPY apps/backend/sky-common ./apps/backend/sky-common
COPY apps/backend/sky-contract ./apps/backend/sky-contract
COPY apps/backend/sky-offer ./apps/backend/sky-offer
COPY docs/api/contract ./docs/api/contract
WORKDIR /workspace/apps/backend
RUN --mount=type=cache,id=gradle-sky-offer,target=/home/gradle/.gradle \
    gradle :sky-offer:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar sky-offer/build/libs/sky-offer.jar \
        extract --layers --launcher --destination sky-offer/build/extracted

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
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-offer/build/extracted/dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-offer/build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-offer/build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=sky:sky /workspace/apps/backend/sky-offer/build/extracted/application/ ./
EXPOSE 5552
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:5552/actuator/health/liveness | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
