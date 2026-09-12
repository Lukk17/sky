## MODIFIED Requirements

### Requirement: REST resource paths use plural nouns consistently
Every collection endpoint MUST use a plural-noun path segment, as `/offers`, `/messages` and `/bookings` do, and a singular collection path such as `/api/owner/offer` or `/api/message` MUST be renamed to its plural equivalent. Two shapes are deliberately outside the rule rather than exceptions to it. A path segment that names a single-valued sub-resource of one parent stays singular, as the owner of one offer does at `/offers/{offerId}/owner`, because pluralising it would claim an offer has several owners. A path segment that is a namespace rather than a collection is likewise not pluralised, as the owner-scoped prefix in `/owner/offers` is not.

#### Scenario: Auditing REST paths
- **WHEN** an operator inspects the Swagger UI for any service
- **THEN** every collection-style endpoint shows a plural noun in its path, and no singular collection path remains
