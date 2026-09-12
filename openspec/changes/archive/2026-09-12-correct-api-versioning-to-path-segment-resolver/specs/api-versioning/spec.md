## REMOVED Requirements

### Requirement: Header-based API versioning via Spring Framework 7 native mechanism
**Reason**: The mechanism this requirement describes was never built. It names the `X-API-Version` request header as the primary resolver, and that string appears in no Java file, no YAML file, no Bruno request and no OpenAPI contract in this repository, with no commit in any branch having ever added it to one. Commit 77a6f44, which is the commit that adopted native versioning, chose a path-segment resolver instead and stated the reason: keeping the version in the URL left the public addresses at `/api/v1` so the frontend was unaffected. The requirement is therefore a record of a proposal rather than of a contract, and its two scenarios both turn on a header that no client has ever sent.
**Migration**: Nothing is dropped. Every rule it carried, that versioning is configured once through the framework's native mechanism, that a default version applies when the request names none, that a controller declares the version it serves, and that an unsupported version is rejected rather than silently defaulted, is restated against the resolver that ships by the requirement "Path-segment API versioning via the Spring Framework 7 native mechanism", which also records why the header was rejected so the absence is not read as an oversight.

### Requirement: Internal endpoints are versioned and namespaced
**Reason**: The decision this requirement recorded was reversed. A separate `/api/internal` namespace did exist, in `OfferInternalController`, and commit c459904 deleted it, moving the owner lookup to `GET /api/v1/offers/{offerId}/owner` for a stated reason: the version then sits right after `/api` like every other endpoint, and the address carries no access-concern naming. The string `api/internal` now appears in no source or configuration file. The requirement's scenario is doubly stale, because it also asserts that the route is not exposed at the root path, when the route is reachable at the same public prefix as everything else and is protected by an authorization rule rather than by its address.
**Migration**: The protection the namespace was standing in for is not dropped, it is made explicit. The requirement "Service-to-service endpoints share the public version prefix and are guarded by authorization" keeps the versioning rule, replaces the namespace rule with the authorization rule that actually guards the route, and adds the two constraints the namespace had been hiding: that the authenticated rule must be ordered ahead of any broader permit rule on the same resource, and that the calling service forwards the caller's token rather than minting one.

## ADDED Requirements

### Requirement: Path-segment API versioning via the Spring Framework 7 native mechanism
Every REST service (sky-booking, sky-offer, sky-message) MUST resolve the API version from a path segment, configured once in the shared library through `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` so that every service installing it versions the same way. The resolver MUST read the second path segment, MUST treat it as a version only when it reads `v` followed by a digit, and MUST otherwise resolve no version, so a resource name is never mistaken for one. A configured default version MUST apply when the path carries no version, and exactly one version MUST be supported, which is version 1 and is also the default. Every controller class or method MUST declare the version it serves via `@RequestMapping(version = "1")`, which keeps the version a request asks for and the version a handler serves two separate facts the framework compares rather than one fact restated. A request header MUST NOT be the resolver. The header was the design this capability originally carried and it was rejected rather than forgotten: the version is kept in the URL so the public addresses stay `/api/v1/...`, which left the frontend, the published API documents and every saved request working unchanged. Reintroducing a header resolver is therefore a contract change needing its own decision, not a gap to be filled in.

#### Scenario: A path whose second segment is a resource name carries no version
- **WHEN** the version resolver is given a path whose second segment names a resource rather than a version, as `/api/offers/42` does
- **THEN** it resolves no version from the path, and the configured default version 1 applies instead of the segment being read as a version named after the resource

#### Scenario: A public path resolves its version from the path
- **WHEN** a client calls `GET /api/v1/offers`
- **THEN** the version resolves from the second path segment, the request matches the handler that declares version 1, and the response is the v1 contract

#### Scenario: An unsupported version is rejected rather than silently defaulted
- **WHEN** a request asks for a version outside the supported set, as version 2 is
- **THEN** the version strategy raises the framework's invalid-version failure, which carries 400, and the request is not quietly served as the default version

### Requirement: Service-to-service endpoints share the public version prefix and are guarded by authorization
An endpoint that exists for another service to call, such as sky-booking's lookup of an offer's owner, MUST carry the same `/api/v1` version prefix as a public endpoint, and MUST NOT live in a separate URL namespace such as `/api/internal`. The access concern MUST be carried by an authorization rule in front of the route rather than by the address, so the address says what the resource is and the security configuration says who may read it. Where the data belongs to an existing resource, the endpoint MUST be modelled as a sub-resource of it, so an offer's owner is read at `/api/v1/offers/{offerId}/owner` rather than at an address naming the caller or the access level. The authorization rule MUST be ordered ahead of any broader permit rule covering the same resource, because a permit-all on the parent collection would otherwise match the sub-resource first and leave it open. Such an endpoint MUST require an authenticated caller and MUST NOT additionally require a realm role, because the caller is a service acting for a user rather than a person holding a role, and the calling service MUST forward the incoming caller's bearer token rather than minting a credential of its own.

#### Scenario: sky-booking calls sky-offer for offer ownership
- **WHEN** sky-booking resolves the owner of an offer while placing a booking
- **THEN** it calls `GET /api/v1/offers/{offerId}/owner` on sky-offer, and it forwards the bearer token of the caller who asked for the booking

#### Scenario: The sub-resource is closed while the collection above it is open
- **WHEN** an unauthenticated request arrives at `GET /api/v1/offers/{offerId}/owner`
- **THEN** sky-offer answers 401, even though an unauthenticated `GET /api/v1/offers` succeeds, because the authenticated rule on the sub-resource is ordered ahead of the permit rule on the collection
