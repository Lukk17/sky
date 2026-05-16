## 1. Dependencies

- [x] 1.1 Add `flyway-core` and `flyway-mysql` to `gradle/libs.versions.toml`.
- [x] 1.2 Wire them into the convention plugin `sky.spring-service-conventions.gradle.kts` (or per-service if too broad — better per-service since sky-notify has no DB).

## 2. Capture baseline schema per service

- [x] 2.1 Spin up a clean MySQL, run sky-booking with current `ddl-auto: update`, let Hibernate generate the schema.
- [x] 2.2 `mysqldump --no-data --skip-add-drop-table sky | sed -e 's/AUTO_INCREMENT=[0-9]*//g'` (clean defaults) → save as `sky-booking/src/main/resources/db/migration/V1__init.sql`.
- [x] 2.3 Repeat for sky-offer and sky-message.
- [x] 2.4 Manually trim and verify each V1 file: stable column order, no MySQL-specific noise (engine/charset are fine if intentional), Hibernate-specific naming preserved.

## 3. Promote seed SQLs

- [x] 3.1 Move `config/script/sql_commands/sql_offers_insert.sql` content into `sky-offer/src/main/resources/db/migration/R__seed_data.sql` guarded by a `spring.flyway.placeholders.seed = ${SEED_DATA:false}` check, OR mark as repeatable + manual.
- [x] 3.2 Move `sql_messages_insert.sql` similarly into sky-message.
- [x] 3.3 If keeping seeds optional, document the activation env var in README.

## 4. Wire Flyway in application.yml

- [x] 4.1 In each data service's `application.yml`: set `spring.flyway.enabled: true`, `spring.flyway.baseline-on-migrate: true`, `spring.flyway.baseline-version: 1`, `spring.flyway.locations: classpath:db/migration`.
- [x] 4.2 Flip `spring.jpa.hibernate.ddl-auto: validate`.
- [x] 4.3 In `application-test.yml`: keep `ddl-auto: validate` (Flyway applies V1 in tests too). Or `create-drop` only if a service has migration-incompatible tests; address case-by-case.
- [x] 4.4 In `application-local.yml`: same as default; no per-dev `update` mode.

## 5. Delete manual SQL scripts

- [x] 5.1 `git rm config/script/sql_commands/sql_create_schema.sql`.
- [x] 5.2 Once seeds promoted, `git rm config/script/sql_commands/sql_offers_insert.sql sql_messages_insert.sql`.
- [x] 5.3 Decide fate of `config/script/sql_commands/` directory — delete if now empty.

## 6. Documentation

- [x] 6.1 Update root `README.md` "DB configuration" section.
- [x] 6.2 Add note in each service's `README.md` about how to add a migration (`V<n>__short_description.sql` convention).
- [x] 6.3 Add ADR or note in `openspec/specs/db-migrations/spec.md` after archive about migration naming and review process.

## 7. Verify

- [x] 7.1 Clean MySQL → start each service → `flyway_schema_history` table appears with V1 applied.
- [x] 7.2 Existing tests pass (H2 with MySQL mode runs the same V1 SQL).
- [x] 7.3 Intentionally break: change an entity column type without a migration; startup fails with `validate` error.
- [x] 7.4 Update `config/k8s/_deployment-scripts/` if any script runs SQL manually; remove the step.
