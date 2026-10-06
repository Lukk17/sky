## Why

The repo carries three classes of accidental cruft that pollute history, confuse new contributors, and in one case leak runtime data into source control. Specifically: Kafka broker runtime artifacts were committed at the repo root (`Developmentkafkakafka-logs/`, `Developmentkafkazookeeper-data/`), two services still ship `Dockerrun.aws.json` files from an abandoned AWS Elastic Beanstalk experiment, and `.gitignore` does not exclude Gradle `build/` output. None of this is load-bearing, but every commit until it is removed re-affirms the mess.

## What Changes

- **Delete** `Developmentkafkakafka-logs/` and `Developmentkafkazookeeper-data/` from repo root (committed Kafka/Zookeeper data directories from a broken docker-compose volume mount path).
- **Delete** `sky-message/Dockerrun.aws.json` and `sky-offer/Dockerrun.aws.json` (AWS Elastic Beanstalk artifacts; not referenced by any active build, CI, or deploy script).
- **Add** `build/` to `.gitignore` (currently only `.gradle/` is ignored; nothing tracked today, but the gap invites a future accident).
- **Add** Kafka/Zookeeper data path patterns to `.gitignore` (`**/kafka-logs/`, `**/zookeeper-data/`, plus the malformed `Developmentkafka*` patterns for safety).
- Fix the docker-compose volume mount in `config/local-dev/` (or wherever it lives) that produced the malformed `Developmentkafka...` directory names — the leading `D:/Development/...` Windows path got concatenated into a relative volume name. Out of scope if the broker isn't in compose at all today (verify during apply).

## Capabilities

### New Capabilities
- `repo-hygiene`: Conventions for what is allowed in source control — no runtime data, no abandoned deploy artifacts, gitignore covers all build outputs and broker state.

### Modified Capabilities
- _None._ No runtime behavior changes.

## Impact

- **Repo size**: shrinks (the committed Kafka logs are likely several MB).
- **Local dev**: anyone with the broken docker-compose mount path needs to recreate their local broker volume. Documented in the change's `tasks.md`.
- **CI/CD**: zero impact — no pipeline references these files.
- **Service code**: zero impact.
- **Risk**: lowest of the 16 changes. Safe to ship first as a warm-up.
