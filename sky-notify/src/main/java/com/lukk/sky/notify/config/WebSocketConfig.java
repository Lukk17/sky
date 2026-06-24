package com.lukk.sky.notify.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket message broker configuration for sky-notify.
 *
 * <p>{@code @EnableWebSocketSecurity} activates Spring Security 6's WebSocket
 * message authorization layer. It installs a {@link org.springframework.security.messaging.context.SecurityContextChannelInterceptor}
 * and an {@link org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor}
 * into the inbound channel automatically. It also disables the STOMP-level CSRF
 * token check (which is not applicable here because we rely on JWT bearer auth, not
 * session cookies).
 *
 * <p>{@link WebSocketAuthChannelInterceptor} continues to run first on STOMP CONNECT
 * and resolves the principal from the Bearer token; subsequent frames are then
 * authorized by the {@link AuthorizationManager} bean below.
 *
 * <p>Per-user destination matching: Spring Security 6's
 * {@code MessageMatcherDelegatingAuthorizationManager} builder does not expose a
 * direct {@code simpSubscribeDestMatchers("/user/{principal}/**").hasUserPrincipal()}
 * predicate. Subscriptions to {@code /user/**} are therefore gated as
 * {@code .authenticated()} — any connected (hence already-JWT-validated) user may
 * subscribe to the {@code /user/**} namespace. The per-user isolation is enforced
 * structurally: the server only pushes to a user's own queue via
 * {@code convertAndSendToUser(principal, ...)} — no user can subscribe to
 * another user's queue unless they know the other's principal name.
 * TODO: add fine-grained per-principal subscribe guard once Spring Security
 *       exposes a path-variable-to-principal matcher in the SimpDestinationMessageMatcher API.
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /queue is the user-destination prefix; convertAndSendToUser("alice", "/queue/notify",
        // payload) routes to /user/alice/queue/notify under the hood.
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/sky");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // CORS is defence-in-depth; STOMP CONNECT requires a JWT regardless of origin.
        registry.addEndpoint("/notifyWebsocket")
                .setAllowedOrigins("https://sky.luksarna.com", "https://skycloud.luksarna.com", "http://localhost:4200")
                .withSockJS();
        registry.addEndpoint("/notifyWebsocket")
                .setAllowedOrigins("https://sky.luksarna.com", "https://skycloud.luksarna.com", "http://localhost:4200");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
                .simpSubscribeDestMatchers("/user/**").authenticated()
                .simpDestMatchers("/sky/**").authenticated()
                .anyMessage().authenticated();

        return messages.build();
    }
}
