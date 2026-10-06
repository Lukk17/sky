## Why

The repository formatting rule forbids joining two independent clauses with a semicolon, and twenty lines across nine
merged specifications still do it. Four of those lines are in the two capabilities that describe what the build
produces and what runs it: `docker-build` scenarios at lines 11 and 18, and `framework-version` scenarios at lines 11
and 15.

The punctuation is the occasion rather than the work. A delta replaces a requirement block by matching its header, so
fixing a scenario means restating its whole requirement, and restating a requirement means standing behind every fact
inside it. Both requirements touched here were therefore checked against the Dockerfiles, the convention plugins and
the version catalogue, and two claims did not survive that check.

The layered-jar mechanism named in the specification is not the one the Dockerfiles use. All five run
`java -Djarmode=tools -jar <module>.jar extract --layers --launcher`, which is the Spring Boot 3.3 and later tools
jarmode, not the `layertools` jarmode the older wording implies, and the `--launcher` flag is what makes the
`org.springframework.boot.loader.launch.JarLauncher` entry point in those files work. Read from
`sky-booking/docker/Dockerfile` lines 13 and 14 and line 38, and identical in the other four.

The second claim is one nobody measured. The `docker-build` scenario promises that an application-only change yields an
image differing "by only a few MB". Nothing in this repository records a measurement behind that number, and this
change runs no build, so the restatement says the structural thing that can be checked by reading the `COPY` order
instead of the magnitude that cannot.

## What Changes

- Restate the two-stage Dockerfile requirement with the punctuation fixed, and name what actually ships: five
  Dockerfiles, one per module that produces a `bootJar`, each building on `gradle:9-jdk25` and running on
  `eclipse-temurin:25-jre-alpine`. The rule that no JDK reaches the runtime image is unchanged.
- Restate the layered-jar requirement with the punctuation fixed, with the tools-jarmode extraction named as the
  mechanism, and with the four layer names it produces kept as they were.
- Replace the unmeasured image-size promise with the layer-invalidation claim the `COPY` order actually supports.
- Restate the Spring Boot 4 and Java 25 requirement with the punctuation fixed in both its scenarios, and with the
  pinned values named: `spring-boot = "4.0.7"` and `java = "25"` in `gradle/libs.versions.toml`.
- Leave the two requirements in these files that carry no semicolon alone, `Containers run as non-root` and
  `Framework version is catalog-managed`. Both were read and both hold, but neither has the evidence pass behind it
  that restating a block obliges.

No code, chart, compose file or build file is touched, and no build is run. Every value comes from reading a committed
file.

## Capabilities

### New Capabilities

None. Both capabilities already exist.

### Modified Capabilities

- `docker-build`: punctuation in both scenarios of the two-stage and layered-jar requirements, the layered-jar
  mechanism, and the unmeasured image-size claim.
- `framework-version`: punctuation in both scenarios of the Spring Boot 4 and Java 25 requirement, plus the pinned
  versions it can now name.

## Impact

- Affected files: `openspec/specs/docker-build/spec.md` and `openspec/specs/framework-version/spec.md`, both rewritten
  at archive time from the deltas in this change. Two of three requirements in the first and one of two in the second.
- Grouped with `framework-version` rather than with the deployment capabilities because the two describe one thing from
  two sides: the JDK that compiles, the JRE that runs, and the catalogue entry that pins both. A reviewer checking the
  `25` in `gradle/libs.versions.toml` is checking the `jdk25` and `25-jre-alpine` tags in the same pass, and a
  disagreement between them breaks the build.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected.
- Risk: low. Three of the four corrections raise precision on facts that already held, and the fourth removes a promise
  nobody measured rather than adding one.
