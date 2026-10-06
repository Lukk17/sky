# api-contract Specification

## Purpose
Fixes where a published API document comes from, so the contract a caller reads is produced by the service that answers the call and cannot drift into describing something else. It governs the document. The addresses inside it, and the version literal that reaches them, belong to api-versioning, and the shape of an error body belongs to sky-common.

## Requirements

### Requirement: A published API document is produced from the service, not maintained beside it
Each REST service MUST publish one API document, and that document MUST be produced from the service itself rather than written and kept in step by hand, so the contract a caller reads cannot disagree with the service that answers the call. Everything the document says MUST therefore be reachable from the service, which today means the mapping annotations, the wire types and the shared response annotations, so a fact that belongs in the published contract is added to the code that serves it and never to the document. A published document MUST NOT be edited directly. The next production overwrites the edit, and an edit that survives long enough to be read is a promise no service is keeping.

A published document that the repository keeps MUST be the output the current source produces, so producing it again yields no difference. That is the whole of the check, and it is what makes the guarantee observable rather than asserted: a difference means the service and its contract have moved apart, and it names in the repository's own reviewable form exactly what a caller would see change. A difference MUST be read as a change to a published contract and reviewed as one, alongside the change to the code that caused it, rather than refreshed as noise.

The coupling runs one way and only one way. A change to a controller changes the published contract with no line of the document being touched, which is the property this requirement buys and also its one cost, so a declaration a contributor believes to be cosmetic is not: the requirement on the version literal in the `api-versioning` capability is the case where that has already cost something, and it is stated there rather than here because it is a rule about the address, not about the document.

#### Scenario: A wire change reaches the contract through the code
- **WHEN** a contributor changes the wire shape of an endpoint and produces the documents again
- **THEN** the document for that service differs, the difference is reviewed with the change that caused it, and no line of the document was edited to make it appear

#### Scenario: An edit to a published document does not survive
- **WHEN** somebody edits a published document directly, to describe behaviour the service does not have or to correct behaviour it does
- **THEN** the next production overwrites the edit, and the only route to publishing that behaviour is to change the service

#### Scenario: The kept copy and the service agree
- **WHEN** the documents are produced again from an unchanged source tree
- **THEN** no file differs, which is the observable form of the contract and the service agreeing, and any file that does differ is a document nobody produced again after the change that moved it
