# Build from repo root:
#   docker build -f sky-notify/docker/Dockerfile -t sky-notify:latest .

# ===============> BUILD STAGE <===============
FROM gradle:9-jdk25 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY sky-common ./sky-common
COPY sky-notify ./sky-notify
RUN --mount=type=cache,id=gradle-sky-notify,target=/home/gradle/.gradle gradle :sky-notify:bootJar --no-daemon -x test \
 && java -Djarmode=tools -jar sky-notify/build/libs/sky-notify.jar \
        extract --layers --launcher --destination sky-notify/build/extracted

# ===============> RUN STAGE <===============
FROM eclipse-temurin:25-jre-alpine AS runtime

# Trust the local Keycloak self-signed certificate so the OAuth2 resource server
# can reach https://keycloak.test:9443 for OIDC discovery and JWKS.
COPY config/keycloak/certs/localhostDomain.crt /tmp/keycloak.crt
RUN keytool -importcert -noprompt \
      -alias keycloak-local \
      -file /tmp/keycloak.crt \
      -keystore "$JAVA_HOME/lib/security/cacerts" \
      -storepass changeit \
 && rm /tmp/keycloak.crt

RUN addgroup -S -g 1000 sky && adduser -S -u 1000 sky -G sky
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
