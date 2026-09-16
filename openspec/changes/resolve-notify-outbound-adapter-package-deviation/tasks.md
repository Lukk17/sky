## 1. Bind the origin list before renaming anything

- [x] 1.1 Read how `CorsConfig` in sky-booking, sky-offer and sky-message binds `sky.crossOrigin.allowed` and splits
  it, and read commit `4970536` which gave those three the chart half, so this module follows a shape that exists
  rather than a new one
- [x] 1.2 Take the origin list into `WebSocketConfig` through the constructor, split on the same expression the three
  REST services use, and delete the constant both STOMP endpoints were reading
- [x] 1.3 Add the committed default to `sky-notify/src/main/resources/application.yaml`, naming the production
  frontend and the two local origins and dropping the API host the constant carried
- [x] 1.4 Rewrite `WebSocketOriginIntegrationTest` so the accepted origins come from the bound property and two
  refused origins are asserted, one of them an origin the deleted constant named
- [x] 1.5 Run the rewritten test against the previous implementation by restoring the constant, confirm it fails in
  both directions, and restore the bound version

## 2. Give the chart the matching half

- [x] 2.1 Add `crossOrigin.allowed` to `values.yaml` empty, to `values-local.yaml` as the two local origins and to
  `values-prod.yaml` as the production frontend
- [x] 2.2 Add `ACCESS_CONTROL_ALLOW_ORIGIN` to the deployment template behind the same `required` message the three
  REST services use
- [x] 2.3 Render the chart three ways and confirm the no-overlay render fails naming `crossOrigin.allowed`, and that
  the local and production renders carry the right value
- [x] 2.4 Diff the local render against the render of the chart at `HEAD` and confirm the only difference is the new
  environment entry

## 3. Rename the package and hold the name with a rule

- [x] 3.1 Move `WebSocketService` from `adapters/outbound/service` to `adapters/outbound/websocket` with `git mv`, fix
  its package declaration and the two imports that name it
- [x] 3.2 Search the tree for the old package name and confirm nothing outside the specifications and the IDE
  inspection exports still refers to it
- [x] 3.3 Add `WebSocketServiceTest` in the new package, asserting the message goes to the target user's own queue
- [x] 3.4 Narrow the `SimpMessagingTemplate` rule from `adapters.outbound` to `adapters.outbound.websocket`
- [x] 3.5 Add the rule forbidding a package under `adapters` named `service`, `impl` or `util`
- [x] 3.6 Prove both rules bite, with one violation each injected in the same run so each failure is readable on its
  own, then remove both violations and confirm the tree is clean

## 4. Write the delta specifications

- [x] 4.1 Copy the requirement `Canonical package layout per service` from the merged file, name the websocket
  package among the examples, record the two new rules and say plainly that the second is an approximation
- [x] 4.2 Remove the deviation paragraph and drop the deviation from the audit scenario that lists it
- [x] 4.3 Add a scenario for the two new rules
- [x] 4.4 Copy the requirement `Cross-origin configuration never allows every origin` from the hygiene capability and
  correct the clause calling the sky-notify list a hardcoded list in configuration code
- [ ] 4.5 Verify neither delta holds an em dash, an en dash, a semicolon joining two clauses, or bold or italic
  outside the `**WHEN**` and `**THEN**` markers, with a matcher validated against a fixture holding both dashes plus
  an arrow and a bullet
- [ ] 4.6 Run `openspec validate resolve-notify-outbound-adapter-package-deviation --strict` and verify it reports no
  error

## 5. Update the module documentation

- [x] 5.1 Record the bound origin list, the chart values per environment and the renamed package in
  `sky-notify/AGENTS.md`, and the two new rules in its conventions section
- [x] 5.2 Add the origin change to the 2.0.0 entry in `sky-notify/CHANGELOG.md`, replacing the bullet that named one
  origin joining a list that no longer exists in the code

## 6. Verify

- [ ] 6.1 Run `./gradlew build` from the repository root and verify it is green, including
  `jacocoTestCoverageVerification` at 0.90 line and 0.90 branch
- [ ] 6.2 Rebuild only the sky-notify image, import it into k3d and upgrade only that release with its local overlay,
  then confirm every pod is ready
- [ ] 6.3 Read `ACCESS_CONTROL_ALLOW_ORIGIN` and the resolved property inside the running container, so the bound
  value is evidence rather than a template
- [ ] 6.4 Drive the deployed handshake with an allowed origin and with one the list does not name, and record both
  answers
- [ ] 6.5 Run the Bruno collection against the cluster with `--env k8s` and confirm it does not regress from 19 of 19
  requests and 97 of 97 assertions

## 7. Notes from the run

- The no-overlay render fails on `ingress.service.hosts[].host` before it reaches the deployment template, because
  Helm stops at the first `required` it evaluates. The crossOrigin message is reached by supplying the three earlier
  required values, and it also fires on either overlay with the value removed. Both forms are recorded rather than
  the first one alone, because a reader who runs the plain command sees the ingress message and could conclude the
  new check is absent.
- The Bruno collection never opens a WebSocket, so a green collection run is not evidence for the origin change. The
  handshake was driven separately against the deployed pod in both directions.
