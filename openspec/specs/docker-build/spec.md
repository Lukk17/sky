# docker-build Specification

## Purpose
Defines how a service image is built and run: a build stage separate from the runtime stage, a runtime that ships no compiler, layers ordered so a code change does not invalidate the dependency layer, and a container that does not run as root.

## Requirements

### Requirement: Two-stage Dockerfile with JDK build and JRE runtime
Every module that produces a `bootJar` MUST be built via a two-stage Dockerfile: a build stage based on a Gradle and JDK image, and a runtime stage based on a JRE-only image. Five modules qualify, which is sky-booking, sky-offer, sky-message, sky-notify and sky-gateway, and sky-common has no Dockerfile because it is a library. Every one of the five builds on `gradle:9-jdk25` and runs on `eclipse-temurin:25-jre-alpine`, and the JDK major in the build stage MUST match the Java version the catalogue pins. The full JDK MUST NOT ship in the runtime image.

#### Scenario: Inspecting the runtime image
- **WHEN** an operator runs `docker run --rm <image> java -version` and then `docker run --rm <image> javac -version`
- **THEN** `java -version` succeeds and `javac` is absent, because the runtime stage carries a JRE rather than a JDK

### Requirement: Spring Boot layered jars for cache-efficient pulls
Each Dockerfile MUST ship the application as extracted Spring Boot layers rather than as one fat jar, so dependencies, spring-boot-loader, snapshot-dependencies and application content each land on their own `COPY` layer in that order. The extraction MUST use the tools jarmode, `java -Djarmode=tools -jar <module>.jar extract --layers --launcher`, and the `--launcher` flag is what puts `org.springframework.boot.loader.launch.JarLauncher` on the entry point. An application-only change MUST invalidate only the last and smallest of the four layers.

#### Scenario: Iterative rebuilds on application-only changes
- **WHEN** a developer changes a single class in sky-booking and rebuilds the image
- **THEN** only the application layer is invalidated, the dependencies, spring-boot-loader and snapshot-dependencies layers are reused from cache because they are copied before it, and the rebuilt image shares every layer below the application one with the previous image

### Requirement: Containers run as non-root
Every service runtime image MUST create and switch to a non-root user before `ENTRYPOINT`. The process inside the container MUST NOT run as UID 0.

#### Scenario: Inspecting the container process
- **WHEN** an operator runs `docker exec -it <container> id`
- **THEN** the output reports a non-zero UID (the configured `sky` user), not root
