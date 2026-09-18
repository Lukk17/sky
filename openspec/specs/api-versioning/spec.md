# api-versioning Specification

## Purpose
Keeps every REST endpoint explicitly versioned and consistently named, so a breaking payload change can ship as a new version instead of silently altering the contract callers already depend on.

## Requirements

### Requirement: REST resource paths use plural nouns consistently
Every collection endpoint MUST use a plural-noun path segment, as `/offers`, `/messages` and `/bookings` do, and a singular collection path such as `/api/owner/offer` or `/api/message` MUST be renamed to its plural equivalent. Two shapes are deliberately outside the rule rather than exceptions to it. A path segment that names a single-valued sub-resource of one parent stays singular, as the owner of one offer does at `/offers/{offerId}/owner`, because pluralising it would claim an offer has several owners. A path segment that is a namespace rather than a collection is likewise not pluralised, as the owner-scoped prefix in `/owner/offers` is not.

#### Scenario: Auditing REST paths
- **WHEN** an operator inspects the Swagger UI for any service
- **THEN** every collection-style endpoint shows a plural noun in its path, and no singular collection path remains

### Requirement: Path-segment API versioning via the Spring Framework 7 native mechanism
Every REST service (sky-booking, sky-offer, sky-message) MUST resolve the API version from a path segment, configured once in the shared library through `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` so that every service installing it versions the same way. The resolver MUST read the second path segment, MUST treat it as a version only when it reads `v` followed by a digit, and MUST otherwise resolve no version, so a resource name is never mistaken for one. A configured default version MUST apply when the path carries no version, and exactly one version MUST be supported, which is version 1 and is also the default. Every controller class or method MUST declare the version it serves through the version attribute of its own mapping annotation, which keeps the version a request asks for and the version a handler serves two separate facts the framework compares rather than one fact restated. A request header MUST NOT be the resolver. The header was the design this capability originally carried and it was rejected rather than forgotten: the version is kept in the URL so the public addresses stay `/api/v1/...`, which is what makes the version part of the address a caller writes down rather than a fact carried beside it. That reason was recorded here with a clause saying it left the frontend and every saved request working unchanged, and the clause was true of the address a service serves and false of the address a caller used, because the edge rewrote paths: the address a caller wrote was `/offer/api/offers`, the rewrite behind it supplied the version segment, and what kept those callers working was that rewrite absorbing the new prefix rather than the version being in the URL they wrote. The requirement below removed the rewriting. Doing so moved the published addresses once, so the saved requests changed with them and any client still holding an old address has to follow, and it is what makes the sentence above hold of every route into this stack rather than of a direct call alone. Reintroducing a header resolver is therefore a contract change needing its own decision, not a gap to be filled in.

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

### Requirement: Service-to-service endpoints share the public version prefix and are guarded by authorization
An endpoint that exists for another service to call, such as sky-booking's lookup of an offer's owner, MUST carry the same `/api/v1` version prefix as a public endpoint, and MUST NOT live in a separate URL namespace such as `/api/internal`. The access concern MUST be carried by an authorization rule in front of the route rather than by the address, so the address says what the resource is and the security configuration says who may read it. Where the data belongs to an existing resource, the endpoint MUST be modelled as a sub-resource of it, so an offer's owner is read at `/api/v1/offers/{offerId}/owner` rather than at an address naming the caller or the access level. The authorization rule MUST be ordered ahead of any broader permit rule covering the same resource, because a permit-all on the parent collection would otherwise match the sub-resource first and leave it open. Such an endpoint MUST require an authenticated caller and MUST NOT additionally require a realm role, because the caller is a service acting for a user rather than a person holding a role, and the calling service MUST forward the incoming caller's bearer token rather than minting a credential of its own.

#### Scenario: sky-booking calls sky-offer for offer ownership
- **WHEN** sky-booking resolves the owner of an offer while placing a booking
- **THEN** it calls `GET /api/v1/offers/{offerId}/owner` on sky-offer, and it forwards the bearer token of the caller who asked for the booking

#### Scenario: The sub-resource is closed while the collection above it is open
- **WHEN** an unauthenticated request arrives at `GET /api/v1/offers/{offerId}/owner`
- **THEN** sky-offer answers 401, even though an unauthenticated `GET /api/v1/offers` succeeds, because the authenticated rule on the sub-resource is ordered ahead of the permit rule on the collection

### Requirement: The published address is the address the service serves
The address a caller sends a request to MUST be the address the service serves that endpoint on, and the path of an API request MUST NOT be rewritten by anything in front of a service, the gateway and the ingress included. The edge chooses which service receives a request and changes nothing else about the address, so `GET /api/v1/offers` is `/api/v1/offers` at the gateway on `http://localhost:5777`, at the ingress on the deployed host, and straight at sky-offer, and the only part that differs across the three is what stands in front of the path. A service-naming prefix such as `/offer/api`, `/booking/api` or `/msg/api` MUST NOT be reintroduced, and no other alias whose only purpose is to be translated into the real path may take its place. Headers are outside this rule and MUST NOT be read as covered by it: the gateway's token relay and the headers oauth2-proxy sets are how an authenticated identity reaches a service, and neither one touches a path.

The rule is affordable only because the services own disjoint top-level resources, which makes that disjointness a property to preserve rather than a coincidence to rely on. `offers`, `owner/offers` and `search` are sky-offer's, `bookings` and `user/bookings` are sky-booking's, `messages` is sky-message's, and `/notifyWebsocket` is sky-notify's. A new top-level resource MUST NOT collide with one another service already serves, and it MUST be added to the gateway route and to that service's ingress in the change that adds it, because a path nothing routes is unreachable from outside and nothing fails to say so.

Swagger UI and the api-docs endpoint are the one exception, and their ingresses keep a service-naming prefix and a rewrite because all three services serve them on the same two paths, so a path alone cannot say which service is meant there. The exception is bounded to those two paths and MUST NOT be widened to an API path on the argument that a prefix reads conveniently.

Two things follow, and they are the reason the rule is worth holding rather than a restatement of it. One published API document describes the service at every hop, because a path in it is the path a caller sends whichever way that caller came in, so a document produced from the service cannot be right about a direct call and wrong about the deployed host. And an authorization gate an ingress carries stops depending on how the ingress controller orders two paths that overlap: two API paths on one host that differ in whether they carry the oauth2-proxy `auth-url` and `auth-signin` annotations MUST NOT overlap, and `/api/v1/owner/offers` behind the gate and `/api/v1/offers` in front of it are disjoint strings where the prefixes they replaced were not, `/offer/api` having matched `/offer/api/owner/offers` as well. A path that is a strict extension of another path carrying a different authentication annotation puts that dependency back and MUST NOT be added.

#### Scenario: A path crosses the edge unchanged
- **WHEN** a client sends a routed path that no handler maps, as `GET /api/v1/offers/zzz/not-mapped` is, to the gateway
- **THEN** the problem detail that comes back carries `instance` `/api/v1/offers/zzz/not-mapped`, which is sky-offer reporting the URI it received, so the path reached the service as it was sent

#### Scenario: Every published path matches a route
- **WHEN** a client calls each published path at the gateway, which is `/api/v1/offers`, `/api/v1/offers/{offerId}/owner`, `/api/v1/search`, `/api/v1/owner/offers`, `/api/v1/bookings`, `/api/v1/user/bookings` and `/api/v1/messages`
- **THEN** every one of them matches a route and is forwarded to its service, and none of them is answered 404 by the gateway itself

#### Scenario: A retired service prefix is routed nowhere
- **WHEN** a client calls `/offer/api/anything`, `/booking/api/anything` or `/msg/api/anything` at the gateway
- **THEN** no route matches and the answer is 404, so a retired address is gone rather than quietly still working

#### Scenario: The gate in front of the owner resource does not rest on path ordering
- **WHEN** an operator lists the API ingress paths one host renders and separates those carrying the oauth2-proxy `auth-url` and `auth-signin` annotations from those that do not
- **THEN** no path in either set is a prefix of a path in the other, and no API ingress carries a `rewrite-target` or a `use-regex` annotation, so which ingress answers an owner request is decided by the path rather than by the order the controller puts its locations in

#### Scenario: One document describes the service at every hop
- **WHEN** a caller takes an operation path from a service's published API document and sends it to the gateway, to the ingress on the deployed host, and straight to the service
- **THEN** the path is the same string in all three calls, and only the address in front of the path differs
