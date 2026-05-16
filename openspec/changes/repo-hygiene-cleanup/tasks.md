## 1. Audit before delete

- [x] 1.1 Confirmed Kafka data dirs were untracked (`git ls-files` returned 0).
- [x] 1.2 Confirmed no source references `Dockerrun.aws.json`.
- [x] 1.3 No `docker-compose.yml` or script in `config/` produces the malformed `Developmentkafka*` path. Likely a one-off `docker run -v` with a malformed Windows path.

## 2. Delete

- [x] 2.1 Deleted `Developmentkafkakafka-logs/` and `Developmentkafkazookeeper-data/` from filesystem (untracked, so no `git rm` needed).
- [x] 2.2 `git rm sky-message/Dockerrun.aws.json sky-offer/Dockerrun.aws.json`.

## 3. Tighten .gitignore

- [x] 3.1 Added `build/`, `**/build/`, `**/kafka-logs/`, `**/zookeeper-data/`, `Developmentkafka*`, `Dockerrun.aws.json` patterns to root `.gitignore`.
- [x] 3.2 `git status` shows no tracked files newly ignored.

## 4. Fix the source of the malformed path (if found)

- [x] 4.1 No compose stanza produces the path — confirmed in audit.
- [x] 4.2 Documented in this change's design notes; future Kafka docker runs should use named volumes.

## 5. Verify

- [ ] 5.1 `./gradlew :sky-booking:build` — deferred until `gradle-multi-project` rewires the build; current per-service `gradlew` still works.
- [x] 5.2 `git status` is clean for repo-hygiene paths.
- [x] 5.3 `git ls-files | grep -E "(kafka-logs|zookeeper-data|Dockerrun\.aws)"` returns nothing.
