## ADDED Requirements

### Requirement: No runtime data in source control
The repository MUST NOT track files produced by running infrastructure (Kafka broker logs, Zookeeper state, database files), nor obsolete deploy artifacts for platforms the project no longer targets.

#### Scenario: Kafka broker logs are excluded
- **WHEN** a developer accidentally points a docker-compose volume at a tracked path and produces Kafka log files
- **THEN** the patterns `**/kafka-logs/`, `**/zookeeper-data/`, and the historical malformed `Developmentkafka*` patterns in `.gitignore` keep the files out of `git status`

#### Scenario: Abandoned deploy artifacts are absent
- **WHEN** any agent or human searches the tree for `Dockerrun.aws.json` (AWS Elastic Beanstalk config)
- **THEN** zero files match, because the project no longer targets Elastic Beanstalk

### Requirement: Build outputs are gitignored
The repository MUST gitignore all standard Gradle build outputs so that an accidental `git add .` never stages compiled artifacts.

#### Scenario: Build output never stages
- **WHEN** `./gradlew build` produces `build/` directories in every module
- **THEN** `git status` reports a clean tree (the `build/` pattern in `.gitignore` excludes them)
