## MODIFIED Requirements

### Requirement: One error shape, held by a last resort that is not an advice
Every error a service in this repository answers with a body MUST be an RFC 9457 problem detail served as `application/problem+json`, and that MUST hold for a failure nobody anticipated exactly as it holds for one a service mapped deliberately. One parser MUST cover the whole error surface of an API, so no second body shape may be reachable through a request the service dispatches to one of its own endpoints. An empty body is not a second shape: the 401 and 403 the security filter chain answers carry a status and a `WWW-Authenticate` header and nothing else, which is declared once by the shared response annotation every secured endpoint carries and which the published API documents therefore report rather than assert, because those documents are generated from the annotations on the controllers instead of being maintained by hand. A 403 a service raises from a rule of its own is a different answer on the same status, and it carries a problem detail like every other failure a service maps deliberately.

The guarantee is bounded, and the bound is the dispatcher. A failure raised in the servlet filter chain never reaches it and still lands on the container error page with that page's flat body. Nothing in this repository throws there today, and the bound is written down so nobody reads the guarantee as wider than it is. It is not licence for a second shape inside the covered surface.

The handler of last resort MUST NOT be a controller advice. It MUST be registered outside the advice set and sorted behind the whole of it, so every mapping a service declares deliberately answers first and the catch-all sees only what every other handler declined. An ordering annotation MUST NOT be accepted as a substitute, because the ordering it would rest on does not exist: advice resolution returns from the first advice whose handler matches the exception at all, and an advice that declares no order already sits at the lowest precedence value there is, so nothing can be made to sort behind the three services' own advices and a tie between them falls back to bean discovery order. A catch-all inside the advice set would therefore be free to take a 503, a 502, a 409 or a 404 away from the handler that meant it, and it would do so silently. Registering it as a `HandlerExceptionResolver` behind the composite that holds the entire advice set gives the ordering structurally, which is why its position, and not only its behaviour, is what this requirement fixes.

The last-resort answer MUST be 500 and MUST disclose nothing about the cause: no exception type, no exception message, no class name and no package name, because an unanticipated failure is the case whose message is likeliest to carry something internal. It MUST carry no `Retry-After`, because nothing at that point knows that waiting would help. It MUST log the failure at error with the full stack, because resolving an exception stops the container logging it and this becomes the only place the failure is visible, and the response MUST carry the correlation id that log line carries.

#### Scenario: A failure nobody mapped
- **WHEN** a request to an endpoint of a service fails with an exception neither that service nor `sky-common` declares a handler for, such as a null dereference inside an offer edit
- **THEN** the caller receives 500 as `application/problem+json`, with `title` `Internal Server Error`, an `instance` naming the request path and a fixed `detail` that names neither the exception type nor its message, and the service logs the failure at error with the full stack under the correlation id the response header carries

#### Scenario: A deliberate mapping still wins
- **WHEN** a booking request fails because sky-offer is unreachable, which sky-booking maps to 503 with `Retry-After: 10`
- **THEN** the caller receives that status, that header and that handler's own `detail`, the last-resort 500 answers nothing, and the same holds for every other status the three REST services map, 502, 409, 404 and 400 included

#### Scenario: A catch-all written as an advice
- **WHEN** a contributor proposes the handler of last resort as a `@ControllerAdvice` or `@RestControllerAdvice`, at any order value
- **THEN** the design is rejected, because no order value sorts behind an advice that declares none, so the catch-all could no longer be guaranteed to run last and the first deliberate mapping it shadowed would surface as a wrong status in production rather than as a failing build

#### Scenario: A consumer that is not a REST service
- **WHEN** a module depends on `sky-common` without exposing a REST surface, as sky-notify does with a servlet container it runs only for a WebSocket handshake, and as sky-gateway does on a reactive stack carrying no servlet API at all
- **THEN** the shared error handling contributes nothing: sky-notify holds the beans and routes no request of its own through them, sky-gateway never loads the auto-configuration and cannot link the types, and both contexts start
