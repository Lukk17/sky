## MODIFIED Requirements

### Requirement: Path-segment API versioning via the Spring Framework 7 native mechanism
Every REST service (sky-booking, sky-offer, sky-message) MUST resolve the API version from a path segment, configured once in the shared library through `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` so that every service installing it versions the same way. The resolver MUST read the second path segment, MUST treat it as a version only when it reads `v` followed by a digit, and MUST otherwise resolve no version, so a resource name is never mistaken for one. A configured default version MUST apply when the path carries no version, and exactly one version MUST be supported, which is version 1 and is also the default. Every controller class or method MUST declare the version it serves through the version attribute of its own mapping annotation, which keeps the version a request asks for and the version a handler serves two separate facts the framework compares rather than one fact restated. A request header MUST NOT be the resolver. The header was the design this capability originally carried and it was rejected rather than forgotten: the version is kept in the URL so the public addresses stay `/api/v1/...`, which left the frontend and every saved request working unchanged. Reintroducing a header resolver is therefore a contract change needing its own decision, not a gap to be filled in.

The literal that declaration carries MUST be the text the public path carries in its version segment, which today is `v1` and not `1`, and that is a constraint on the text rather than on the version. At run time the two are one value, because the framework skips leading non-digit characters before it parses, so either literal matches the same request, satisfies the same configured default and joins the supported set as the same version. In a published API document they are not one value, because that document is generated from these annotations rather than written by hand: the version segment of every operation path in it is rewritten with the declared literal exactly as declared, so a service that serves `/api/v1/offers` and declares `1` publishes `/api/1/offers`, on every operation it exposes. The literal is therefore load bearing for a contract a caller reads, and changing it changes that contract without changing one response, which is why this requirement fixes the text and does not leave it to whatever reads naturally at the declaration site. What the published document must agree with is the subject of the `api-contract` capability, and this is the requirement that names the text which makes it agree.

#### Scenario: A path whose second segment is a resource name carries no version
- **WHEN** the version resolver is given a path whose second segment names a resource rather than a version, as `/api/offers/42` does
- **THEN** it resolves no version from the path, and the configured default version 1 applies instead of the segment being read as a version named after the resource

#### Scenario: A public path resolves its version from the path
- **WHEN** a client calls `GET /api/v1/offers`
- **THEN** the version resolves from the second path segment, the request matches the handler that declares version 1, and the response is the v1 contract

#### Scenario: An unsupported version is rejected rather than silently defaulted
- **WHEN** a request asks for a version outside the supported set, as version 2 is
- **THEN** the version strategy raises the framework's invalid-version failure, which carries 400, and the request is not quietly served as the default version

#### Scenario: The declared literal is the one the published document carries
- **WHEN** the API document for a service is produced from that service's controller annotations
- **THEN** the version segment of every operation path in it reads the declared literal exactly, so it reads `v1` and names an address the service answers, and a declaration reading `1` would publish `/api/1/...` for a service still serving `/api/v1/...`
