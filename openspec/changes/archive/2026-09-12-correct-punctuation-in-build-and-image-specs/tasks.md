## 1. Establish the punctuation scope

- [x] 1.1 Count the semicolon lines in `openspec/specs/docker-build/spec.md` and
  `openspec/specs/framework-version/spec.md` and verify the count is two in each
- [x] 1.2 Read each of the four lines and classify it as two independent clauses, a serial list separator or shell
  syntax, so a list separator is not rewritten as though it were a comma splice
- [x] 1.3 Record which requirement block each line belongs to, since a scenario cannot be corrected without restating
  its requirement

## 2. Check the Docker claims against the five Dockerfiles

- [x] 2.1 List every Dockerfile in the repository and verify there are five, one per module that produces a `bootJar`,
  and none for sky-common
- [x] 2.2 Record the build-stage and runtime-stage base images of all five and verify they agree
- [x] 2.3 Verify the runtime stage carries no JDK, by confirming the base image is a JRE image
- [x] 2.4 Record the layer extraction command and verify which jarmode it uses, because the requirement names a
  mechanism and the older `layertools` one would be the wrong name
- [x] 2.5 Verify the four extracted layers are copied in the order the requirement states, and that the application
  layer is copied last
- [x] 2.6 Verify the entry point matches the extraction flags, so the `--launcher` claim is not asserted without its
  consequence
- [x] 2.7 Decide what to do about the image-size promise, given that this change runs no build and no measurement for
  it exists in the repository
- [x] 2.8 Read the requirement this change does not touch, `Containers run as non-root`, and record whether it holds,
  without restating it

## 3. Check the framework claims against the build files

- [x] 3.1 Record the pinned Spring Boot and Java versions from `gradle/libs.versions.toml`
- [x] 3.2 Verify no module overrides the toolchain or pins its own framework version
- [x] 3.3 Verify the toolchain is resolved from the catalogue entry in the shared convention plugin
- [x] 3.4 Verify no source file imports the Spring Boot 3 `MockBean` annotation, and that `MockitoBean` is what tests
  use
- [x] 3.5 Establish whether the deprecation status of `@MockBean` under Spring Boot 4 can be verified without running
  a build, and if not, state only what a grep can check

## 4. Write the delta specifications

- [x] 4.1 Read both current merged specifications rather than assuming their content
- [x] 4.2 Write `specs/docker-build/spec.md` and `specs/framework-version/spec.md` under `## MODIFIED Requirements`,
  carrying each whole requirement block
- [x] 4.3 Verify every requirement header and every existing scenario name matches the merged file character for
  character, so the archive step cannot drop a scenario
- [x] 4.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic
  outside the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against
  a fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 4.5 Verify both deltas respect the wrap width of their merged files, which is one physical line per paragraph and
  per scenario bullet
- [x] 4.6 Run `openspec validate correct-punctuation-in-build-and-image-specs --strict` and verify it reports no error

## 5. Archive and verify the merged files

- [x] 5.1 Archive the change and verify both merged specifications carry the corrected blocks
- [x] 5.2 Verify no semicolon joining two clauses remains in either merged file
- [x] 5.3 Verify the two untouched requirements are byte-identical to what they were before the archive
- [x] 5.4 Verify `openspec validate --specs --strict` still reports eighteen passed and none failed
- [x] 5.5 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 6. Notes from the run

- Task 1.2 classification. All four lines join two independent clauses and all four were corrected. None of the four is
  a serial list separator and none is shell syntax, which was checked by reading rather than by pattern, because the
  same pass over the other seven specifications did find one line where the semicolons are a legitimate serial
  separator.
- Task 2.4 found the stale mechanism. All five Dockerfiles run `java -Djarmode=tools -jar <module>.jar extract --layers
  --launcher`, the tools jarmode. The requirement said only "Spring Boot's layered-jar mode", which a reader would most
  likely look for as the older `layertools` jarmode that Spring Boot no longer carries.
- Task 2.5 holds in all five files. Read in `sky-booking/docker/Dockerfile` at lines 30 to 33: dependencies,
  spring-boot-loader, snapshot-dependencies, application, in that order, application last. The other four carry the
  same four lines with their own module path substituted.
- Task 2.6 holds. The entry point is `ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]`, which
  is the launcher the `--launcher` extraction flag puts in place.
- Task 2.7 decided to drop the magnitude. No measurement of the rebuilt image delta exists anywhere in the repository,
  and this change runs no build, so the scenario now states the layer invalidation that the `COPY` order supports.
- Task 2.8 read `Containers run as non-root` without restating it. It holds in all five Dockerfiles, each of which runs
  `addgroup -S -g 1000 sky && adduser -S -u 1000 sky -G sky` and then `USER sky` before the entry point.
- Task 3.2 holds. No module build file mentions a toolchain or a `JavaLanguageVersion`, so the only declaration is the
  one in `buildSrc/src/main/kotlin/sky.java-conventions.gradle.kts`, which reads `libs.versions.java`.
  `gradle/gradle-daemon-jvm.properties` separately pins `toolchainVersion=25` for the daemon.
- Task 3.4 holds. The deprecated import appears zero times across every module source tree, and four test classes use
  `MockitoBean`.
- Task 3.5 could not be settled without resolving the dependency, so the requirement claims only what a grep checks.
  Whether Spring Boot 4.0.7 still ships `@MockBean`, deprecates it or has removed it is not asserted.
- Task 5.3 needs one clarification. The two untouched requirement blocks are byte-identical in their own text, which was
  confirmed by diffing the merged files. The archive step does, however, normalise the whitespace around the
  `## Purpose` and `## Requirements` headings and drops the trailing blank line, so both files show three cosmetic
  whitespace lines in the diff that belong to the tool rather than to this change. Every specification previously
  rewritten by an archive carries the same normalisation, and the ones never rewritten do not, which is how that was
  established.
- `openspec archive` emitted the same non-blocking warning as the changes archived before it today, that the proposal's
  Why section exceeds 1000 characters. Left as written, because each corrected claim needs the file and the line it was
  read from.
