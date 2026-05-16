## 1. Audit before delete

- [ ] 1.1 `git log --all -- Developmentkafkakafka-logs/ Developmentkafkazookeeper-data/` — confirm no useful history we would lose.
- [ ] 1.2 `grep -r "Dockerrun.aws.json" .` — confirm nothing references the EB files.
- [ ] 1.3 `grep -r "Developmentkafka" .` — find any compose/script that produced the malformed paths.

## 2. Delete

- [ ] 2.1 `git rm -r Developmentkafkakafka-logs/ Developmentkafkazookeeper-data/`.
- [ ] 2.2 `git rm sky-message/Dockerrun.aws.json sky-offer/Dockerrun.aws.json`.

## 3. Tighten .gitignore

- [ ] 3.1 Append `build/`, `**/kafka-logs/`, `**/zookeeper-data/`, `Developmentkafka*` to root `.gitignore`.
- [ ] 3.2 Verify `git status --ignored` shows expected patterns and nothing tracked is now ignored.

## 4. Fix the source of the malformed path (if found)

- [ ] 4.1 If a `docker-compose.yml` under `config/local-dev/` or `config/docker/` has a volume like `./Development/kafka/...` that resolves to the broken path on Windows, replace with a named volume (`kafka_data:/var/lib/kafka/data`) and declare it under `volumes:`.
- [ ] 4.2 If no such compose stanza exists, document in change-notes that the dirs were orphaned artifacts and no source fix is required.

## 5. Verify

- [ ] 5.1 `./gradlew :sky-booking:build` (or equivalent) — repo still builds.
- [ ] 5.2 `git status` is clean.
- [ ] 5.3 `git ls-files | grep -E "(kafka-logs|zookeeper-data|Dockerrun\.aws)" ` returns nothing.
