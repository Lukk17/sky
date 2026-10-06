## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops
a scenario name the merged requirement has. All four existing scenario names are carried through unchanged and no
scenario is added, so every block here is a restatement and none is a rename.

No build is run by this change, which bounds what the restatements are allowed to claim. Every value in them comes from
reading a committed file: the five Dockerfiles, `buildSrc/src/main/kotlin/sky.java-conventions.gradle.kts` and
`gradle/libs.versions.toml`.

## Goals / Non-Goals

**Goals:**

- Clear four semicolon-joined clauses without softening any rule they carried.
- Replace the one promise in these two capabilities that rests on no measurement, because a specification that asserts
  a magnitude nobody measured teaches a reader to distrust the ones that are real.
- Name the mechanism the Dockerfiles use, so the next reader who greps for it finds it.

**Non-Goals:**

- `Containers run as non-root` and `Framework version is catalog-managed`. Neither carries a semicolon, so neither has
  to be restated, and restating a block obliges correcting every stale detail inside it. Both were read and both hold:
  all five Dockerfiles create a `sky` user at UID 1000 and switch to it before the entry point, and the Spring Boot
  version is declared once in the catalogue. Neither claim had the full evidence pass a restatement would require.
- Any change to a Dockerfile, a convention plugin or the catalogue. The specifications are being aligned to files that
  already ship.

## Decisions

Name the image tags rather than the vendor alone. The old text said "eclipse-temurin or distroless", which permits a
runtime nobody uses and says nothing about the major version. Writing `gradle:9-jdk25` and
`eclipse-temurin:25-jre-alpine` means the requirement goes stale loudly on the next JDK bump instead of silently, which
is the same reasoning that put
`postgres:17-alpine` into `test-strategy` earlier today. The abstract rule survives alongside it: a JRE-only runtime,
and no JDK in the runtime image.

Say "every module that produces a `bootJar`" rather than "every service". Five modules carry a Dockerfile and one of
them, sky-gateway, is not a deployed service at all: it is the local-development proxy, it ships no Helm chart, and it
still builds an image that has to obey these rules. Saying "service" left a reader to guess whether the gateway counted.
sky-common is excluded by the same phrasing for the right reason, which is that a library produces no `bootJar`.

Replace the image-size promise with a layer-invalidation claim. "The resulting image differs by only a few MB" is a
measurement, and no measurement exists for it in this repository. What the `COPY` order does support, by reading
`sky-booking/docker/Dockerfile` lines 30 to 33, is that the application layer is copied last and therefore is the only
one an application-only change invalidates. That is checkable by reading the file, which is the standard the rest of
this change is held to. The weaker claim is also the more useful one, because it names the property that makes the pull
cheap rather than a consequence of it that varies with the application.

Keep the `@MockBean` rule as a prohibition on the import rather than as a claim about its deprecation status. The old
text called it a deprecated Spring Boot 3 annotation. Whether Spring Boot 4.0.7 still ships the class, deprecates it or
has removed it was not established here, and it cannot be without resolving the dependency, so the requirement states
the part that is checkable with a grep and that the build already satisfies: the import must not appear, and
`@MockitoBean` is what a test uses instead.

## Risks / Trade-offs

Pinning two image tags in a specification duplicates values that live in five Dockerfiles, so a JDK bump now has to
touch both. That is deliberate, and it is the same trade the `postgres:17-alpine` pin accepted: the alternative wording
survived an entire engine or version change without anyone noticing, which is the more expensive failure.

Dropping the "few MB" figure loses a concrete number from the specification. Anyone who wants it back can measure it
and add it with the measurement named. Restoring it without a measurement would put the same unverifiable claim back.

The claim that the four layers are copied in the stated order was read from one Dockerfile and confirmed identical in
the other four by reading the same lines in each. It is not protected by a test, so a future Dockerfile edit could
reorder the copies and this requirement would be the only thing that notices.

## Migration Plan

Archive this change and confirm both merged specifications carry the corrected blocks, that the two requirements this
change does not touch are unchanged, and that no semicolon joining two clauses remains in either file. Rollback is
`git checkout` on the two merged specifications, because nothing is built, deployed or migrated.
