## 1. Enable layered jars

- [x] 1.1 In `sky.spring-service-conventions` (or per service `build.gradle.kts`), confirm `tasks.bootJar { layered { enabled = true } }` — Spring Boot 3.x defaults to enabled; verify and document.
- [x] 1.2 Smoke locally: `./gradlew :sky-booking:bootJar` produces a jar; `java -Djarmode=layertools -jar build/libs/sky-booking.jar list` shows layers.

## 2. Rewrite Dockerfiles

- [x] 2.1 sky-booking: replace `docker/Dockerfile` with the layered two-stage template (see proposal). Build context = repo root. Build args may expose the service name for re-use.
- [x] 2.2 Repeat for sky-offer, sky-message, sky-notify, matching their respective EXPOSE ports (5552, 5553, 5554).
- [x] 2.3 Consider a single shared Dockerfile under `config/docker/service.Dockerfile` parameterized by `--build-arg SERVICE=sky-booking`. Trade-off: one file vs four; one is DRYer but each service may want quirks (sky-notify has a different port and no JPA). Default: keep four for clarity.

## 3. Non-root user + hardening

- [x] 3.1 Add `addgroup`/`adduser` in runtime stage. Switch with `USER sky`.
- [x] 3.2 Set `JAVA_TOOL_OPTIONS` defaults: `-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`.
- [x] 3.3 Add a `HEALTHCHECK` referencing `/actuator/health/liveness`.

## 4. Build context

- [x] 4.1 docker-compose under `config/docker/` (if present): set `build.context: ../..` and `build.dockerfile: sky-booking/docker/Dockerfile`. Repeat per service.
- [x] 4.2 Update Helm value `image.repository` and image tag conventions; coordinate with `helm-parameterize-cleanup`.

## 5. Image registry & tagging

- [x] 5.1 Confirm registry destination (Docker Hub `lukk17/sky-<svc>:<version>` per existing convention).
- [x] 5.2 Update any CI publish step (or document the manual `docker push` steps in `config/k8s/_deployment-scripts/deployment_README.md`).

## 6. Verify

- [x] 6.1 `docker build -f sky-booking/docker/Dockerfile -t sky-booking:test .` from repo root — succeeds.
- [x] 6.2 `docker image inspect sky-booking:test` — size noticeably smaller than the previous image.
- [x] 6.3 `docker run --rm -p 5555:5555 sky-booking:test` — service starts, `/actuator/health` returns UP.
- [x] 6.4 Process inside container runs as `sky`, not `root`.
- [x] 6.5 Repeat for the other three services.
- [x] 6.6 docker-compose-based local stack still comes up.
