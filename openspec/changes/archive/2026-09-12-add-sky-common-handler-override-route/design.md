## Context

See proposal.md, Why, for the motivation. This change exists because the change before it did not do what its own
design said it would, so the relevant constraint is about scope discipline rather than about tooling.

The capability has one requirement, so a MODIFIED delta replaces the whole block including all of its scenarios. That
makes the risk here the opposite of the usual one: the danger is not omitting something, it is quietly improving
something while the stated purpose is one clause. Four scenarios and every other sentence of the requirement were
corrected on evidence earlier today and are not in question.

## Goals / Non-Goals

**Goals:**

- Say how a service replaces a shared handler, so the prohibition on extending one has an alternative beside it.
- Change exactly one clause, and prove the rest was carried verbatim rather than asserting it.

**Non-Goals:**

- Revisiting anything else in the requirement. If a further defect is found in it, that is another change.
- Any code change. The override route already works, it was simply not written down.
- Reopening the archived change this follows. Its record stands, including its unchecked task.

## Decisions

Carry the four existing scenarios by copying them out of the merged file programmatically and comparing them back
byte for byte, rather than by retyping them. A restatement of a five-scenario block for the sake of one clause is
exactly where a transcription error hides, and the comparison is cheap.

Add a scenario for the override rather than leaving the rule only in the requirement body. The body now carries three
normative statements about handlers, and the one a service acts on, declare your own bean, is the one worth having as a
check someone can run against a context.

State the additive case as well as the replacing case. A service that wants extra handling alongside the shared
behaviour does not override anything, it adds a second advice, and that is what `sky-booking`, `sky-offer` and
`sky-message` each do with their own `GlobalExceptionHandler`. Without that sentence the requirement reads as though any
service-side handling displaces the shared one, which would describe three services as overriding when none of them is.

## Risks / Trade-offs

The requirement body is now long enough that a reader may skim it. Mitigation: the handler rules are consecutive and the
one with an action in it is also a scenario.

This change makes two archived changes touch the same requirement on the same day, so the history of that block is in two
places. Mitigation: the follow-up is named in the notes of the change it follows, so reading either one leads to the
other.
