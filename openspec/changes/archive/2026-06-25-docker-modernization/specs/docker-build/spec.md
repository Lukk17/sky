## ADDED Requirements

### Requirement: Two-stage Dockerfile with JDK build and JRE runtime
Every service MUST be built via a two-stage Dockerfile: a build stage based on a Gradle + JDK image, and a runtime stage based on a JRE-only image (eclipse-temurin or distroless). The full JDK MUST NOT ship in the runtime image.

#### Scenario: Inspecting the runtime image
- **WHEN** an operator runs `docker run --rm <image> java -version` and `docker run --rm <image> javac -version`
- **THEN** `java -version` succeeds; `javac` is not present in the image

### Requirement: Spring Boot layered jars for cache-efficient pulls
Each service's Dockerfile MUST use Spring Boot's layered-jar mode: dependencies, spring-boot-loader, snapshot-dependencies, and application content live on separate `COPY` layers. Application-only changes MUST invalidate only the smallest top layer.

#### Scenario: Iterative rebuilds on application-only changes
- **WHEN** a developer changes a single class in `sky-booking` and rebuilds the image
- **THEN** only the application layer rebuilds; the dependency layer is reused from cache; the resulting image differs by only a few MB

### Requirement: Containers run as non-root
Every service runtime image MUST create and switch to a non-root user before `ENTRYPOINT`. The process inside the container MUST NOT run as UID 0.

#### Scenario: Inspecting the container process
- **WHEN** an operator runs `docker exec -it <container> id`
- **THEN** the output reports a non-zero UID (the configured `sky` user), not root
