## Why

Current per-service `docker/Dockerfile`s:
- Build stage: `gradle:8.1.1-jdk17`
- Runtime stage: `openjdk:17` (full JDK in runtime — overkill)
- Fat jar, no layered jar support — every change invalidates the entire image layer.
- JDK 17 lags behind the service's actual Java version (21).

Two-stage is already there; the proposal is to do it *well*:

1. Build stage uses a current `gradle` image matching the project Java version.
2. Runtime stage uses a JRE-only or distroless image — smaller, smaller attack surface.
3. Spring Boot **layered jar** mode plus separate `COPY` layers — only the application classes layer changes on most rebuilds, so registry pulls are tiny.
4. Build context becomes repo root (multi-project Gradle) instead of per-service.

## What Changes

- **Rewrite** each service's Dockerfile as a two-stage build:
  ```
  FROM gradle:8.14.3-jdk21 AS build
  WORKDIR /workspace
  COPY . .
  RUN ./gradlew :sky-<svc>:bootJar --no-daemon -x test
  RUN mkdir -p /workspace/extracted && \
      java -Djarmode=layertools -jar /workspace/sky-<svc>/build/libs/sky-<svc>.jar extract --destination /workspace/extracted

  FROM eclipse-temurin:21-jre-alpine AS runtime
  RUN addgroup -S sky && adduser -S sky -G sky
  USER sky
  WORKDIR /app
  COPY --from=build /workspace/extracted/dependencies/ ./
  COPY --from=build /workspace/extracted/spring-boot-loader/ ./
  COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
  COPY --from=build /workspace/extracted/application/ ./
  EXPOSE 555X
  ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
  ```
- **Switch** runtime base to `eclipse-temurin:21-jre-alpine` (or `gcr.io/distroless/java21` if Alpine + Spring Boot known issues warrant). JRE-only saves ~150MB.
- **Adopt** Spring Boot layered jars (already supported via `BootJar` task; just configure `layered { enabled = true }` if needed).
- **Run as non-root** (`sky` user) — security hygiene.
- **Build context** = repo root (paired with `gradle-multi-project`).
- **When SB4 lands**, bump base images to JDK 25 / JRE 25 — coordinate with `spring-boot-4-migration`.

## Capabilities

### New Capabilities
- `docker-build`: Two-stage Dockerfile per service with JDK build / JRE runtime, layered jars for cache efficiency, non-root user, repo-root build context.

### Modified Capabilities
- _None._

## Impact

- **Touched files**: 4 × `<service>/docker/Dockerfile`. Possibly `docker/docker-compose.yml` if build context paths change.
- **Image size**: drops noticeably (JDK→JRE saves ~150MB; Alpine vs Debian-based saves another ~80MB; rough drop from ~400MB to ~180MB per service).
- **Pull time**: faster after first pull thanks to layered jars (most changes invalidate only the small application layer).
- **CI**: `docker build` from repo root; update any CI step that did `docker build sky-booking/`.
- **K8s**: image names unchanged; tags pinned per service version. Coordinate with `helm-parameterize-cleanup`.
- **Risk**: low-medium. Alpine + glibc-dependent native libs can bite (none expected in this stack, but verify by smoke). Fallback to `eclipse-temurin:21-jre` (non-alpine) if any startup issue surfaces.
- **Dependency order**: depends on `gradle-multi-project` (build context changes). Should land before `spring-boot-4-migration` to keep the JDK bump isolated.
