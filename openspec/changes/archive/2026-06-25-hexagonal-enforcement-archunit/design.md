## Context

ArchUnit is a Java library that loads classes (via classpath scanning, not bytecode rewriting) and asserts properties via JUnit. It's the standard tool for "your architecture diagram, as a test." The rules become living documentation that future contributors trip over the moment they violate them — far better than a wiki page nobody reads.

The current code is *almost* hexagonal. The audit found consistent `adapters/`/`domain/`/`config/` split, port interfaces, and `*ServicePrimary` adapter naming. What's missing is the *enforcement* and a couple of cosmetic normalizations. This change is small in risk but big in compounding value — every future change pays back via faster reviews.

## Goals / Non-Goals

**Goals:**
- Fail the build when someone introduces a layer violation (e.g., controller → repository skip-layer).
- Standardize the package layout across all four services so navigating one means navigating all.
- Make primary (driving) vs secondary (driven) ports explicit through naming or sub-package.

**Non-Goals:**
- Re-architecting any service's domain logic.
- Introducing CQRS, event sourcing, DDD aggregates, or any other heavier pattern. (Event sourcing already partially present via `*Event` entities — leave as-is.)
- Cross-service architectural rules (one ArchUnit class per service is enough; the rules are reused from `sky-common`).
- Imposing rules so strict that they block reasonable Spring usage (e.g., `@Component` on adapters is fine).

## Decisions

1. **One canonical layout**: `adapters.{api,inbound,outbound,persistence}`, `domain.{model,ports,service,exception}`, `config`. Decided here, enforced in tests, never debated in PR review again.
2. **Primary/secondary not expressed via separate sub-packages.** Tried alternatives; `domain.ports.primary` vs `.secondary` adds friction without clarity. Use Javadoc + naming (`*UseCase` for primary, `*Port` for secondary if a service grows enough to warrant the distinction; not required day one).
3. **`*ServicePrimary` naming kept.** It's quirky but consistent. Renaming to `*UseCaseImpl` would touch every service. Not worth the churn now; an ADR captures the choice.
4. **ArchUnit rules live in `sky-common` test-fixtures.** Reusable across services without duplicating the rule definitions. Test-fixtures classifier avoids polluting main jar.
5. **Cross-cutting bans**: no `WebClient`/`RestTemplate` in `domain.*`, no JPA entity in controller signatures. These are the two failures that actually leak architecture in real codebases. Other potential rules (no Spring stereotypes in `domain.*`) are softer — `@Service` on a class under `domain.service` is fine because the package is allowed to know about Spring.
6. **OfferInternalController path move** is bundled here because the layout normalization is the natural moment to fix it.

## Risks / Trade-offs

- **Mass file moves break IDE state**: full re-import once. Documented.
- **ArchUnit boot time** adds a few seconds to the test phase per service. Acceptable.
- **Rule strictness**: too strict and developers disable the test. Mitigate by allowing common pragmatic exceptions (e.g., `@SpringBootTest` setup classes outside the layered rules).
- **Internal API path change** ripples to `sky-booking` and its integration test — coordinated in the same change.
- **Future Spring Boot upgrade** (separate change) may shift package names of Spring types referenced in rules; address there if needed.
