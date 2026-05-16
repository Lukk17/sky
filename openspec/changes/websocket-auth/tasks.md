## 1. Dependencies

- [ ] 1.1 Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` to the catalog.
- [ ] 1.2 Apply to sky-notify only.

## 2. JWT decoder config

- [ ] 2.1 Add `spring.security.oauth2.resourceserver.jwt.issuer-uri: ${AUTH0_ISSUER_URI}` to sky-notify `application.yml`.
- [ ] 2.2 Add a `SecurityConfig` that permits the actuator health/info endpoints and otherwise requires authentication. The WebSocket handshake itself is best left as `permitAll` (auth lives in the STOMP CONNECT interceptor).
- [ ] 2.3 Confirm the JWT issuer matches Auth0 tenant the Ingress already validates (re-use the same env var convention).

## 3. STOMP CONNECT interceptor

- [ ] 3.1 Create `WebSocketAuthChannelInterceptor` implementing `ChannelInterceptor.preSend(Message<?>, MessageChannel)`:
  - If `StompCommand.CONNECT`, read `nativeHeader("Authorization")`, strip `Bearer `, call the configured `JwtDecoder.decode(token)`.
  - On success, build a `JwtAuthenticationToken` and set as `SimpMessageHeaderAccessor.user`.
  - On failure, throw `MessagingException` — STOMP returns ERROR frame to client.
- [ ] 3.2 Register the interceptor in `WebSocketConfig.configureClientInboundChannel`.

## 4. Switch broadcast → per-user

- [ ] 4.1 In `WebSocketConfig.configureMessageBroker`, add `enableSimpleBroker("/queue").setUserDestinationPrefix("/user")`. Keep `/notify` simple-broker destination only if a true broadcast is still needed; recommended to remove.
- [ ] 4.2 In `WebSocketService.triggerMessage`, change signature to `triggerMessage(String username, Object payload)`. Internally call `messagingTemplate.convertAndSendToUser(username, "/queue/notify", payload)`.
- [ ] 4.3 Update `NotificationPublisherPrimary` and `NotificationTransmissionServicePrimary` to pass the user identifier sourced from `KafkaPayloadModel` headers (`USER_EMAIL` header in the WebSocket payload model).
- [ ] 4.4 If `KafkaPayloadModel` does not carry the target user today, add it. Producer side (sky-booking, sky-offer) already has the user via `USER_INFO_HEADERS` constants.

## 5. Tests

- [ ] 5.1 Unit test `WebSocketAuthChannelInterceptor`: valid JWT → principal set; expired JWT → exception; missing header → exception.
- [ ] 5.2 Integration test using `WebSocketStompClient`: connect with valid token → subscribe `/user/queue/notify` → receive expected message. Connect without token → connection refused.
- [ ] 5.3 Add a "no broadcast" assertion: send a per-user message; a second client (different user) subscribed to `/user/queue/notify` does NOT receive it.

## 6. Verify

- [ ] 6.1 sky-view repo (separate) updated to attach `Authorization` STOMP header; document the API change in sky-notify README.
- [ ] 6.2 Smoke test in local: post an offer → only the offer owner's WS client receives the notification.
- [ ] 6.3 Negative smoke: open dev-tools WS console with no token → server closes with ERROR.
- [ ] 6.4 Helm chart values for sky-notify expose `AUTH0_ISSUER_URI` as an env var.
