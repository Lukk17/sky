package com.lukk.sky.notify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

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
 * and resolves the principal from the Bearer token or the gateway session user.
 * Subsequent frames are then authorized by the {@link AuthorizationManager} bean
 * below. This configurer runs at {@link Ordered#HIGHEST_PRECEDENCE} so its
 * interceptor registers before Spring Security's authorization interceptor:
 * channel interceptors run in registration order and neither security
 * interceptor is {@link Ordered}, so without this the authorization check
 * would deny CONNECT before authentication is set.
 *
 * <p>The STOMP CSRF token check that {@code @EnableWebSocketSecurity} installs
 * is replaced with a pass-through by the {@code csrfChannelInterceptor} bean
 * below (the name is the hook Spring Security's own configurer looks up).
 * The check demands a synchronizer token in the WebSocket session, but nothing
 * here mints one because authentication never rides on cookies: direct clients
 * present a JWT bearer on CONNECT and browser clients arrive through the
 * gateway session, whose identity the gateway forwards stripped of
 * client-set copies. Cross-site upgrades are stopped one layer earlier, by
 * the origin list on the endpoint, which rejects any browser origin that is
 * not configured.
 *
 * <p>Per-user destination matching: Spring Security 6's
 * {@code MessageMatcherDelegatingAuthorizationManager} builder does not expose a
 * direct {@code simpSubscribeDestMatchers("/user/{principal}/**").hasUserPrincipal()}
 * predicate. Subscriptions to {@code /user/**} are therefore gated as
 * {@code .authenticated()}: any connected (hence already-JWT-validated) user may
 * subscribe to the {@code /user/**} namespace. The per-user isolation is enforced
 * structurally: the server only pushes to a user's own queue via
 * {@code convertAndSendToUser(principal, ...)}. No user can subscribe to
 * another user's queue unless they know the other's principal name.
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocketSecurity
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }


    private static final String NOTIFY_ENDPOINT = "/notifyWebsocket";
    private static final String ORIGIN_SEPARATOR = "\\s*,\\s*";

    private final WebSocketAuthChannelInterceptor authInterceptor;
    private final GatewayUserHandshakeInterceptor gatewayUserHandshakeInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(WebSocketAuthChannelInterceptor authInterceptor,
                           GatewayUserHandshakeInterceptor gatewayUserHandshakeInterceptor,
                           @Value("${sky.crossOrigin.allowed}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.gatewayUserHandshakeInterceptor = gatewayUserHandshakeInterceptor;
        this.allowedOrigins = List.of(allowedOrigins.split(ORIGIN_SEPARATOR));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /queue is the user-destination prefix. convertAndSendToUser("alice", "/queue/notify",
        // payload) routes to /user/alice/queue/notify under the hood.
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/sky");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // CORS is defence-in-depth. STOMP CONNECT requires a JWT regardless of origin.
        registry.addEndpoint(NOTIFY_ENDPOINT)
                .setAllowedOrigins(allowedOrigins())
                .addInterceptors(gatewayUserHandshakeInterceptor)
                .withSockJS();
        registry.addEndpoint(NOTIFY_ENDPOINT)
                .setAllowedOrigins(allowedOrigins())
                .addInterceptors(gatewayUserHandshakeInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Bean("csrfChannelInterceptor")
    public ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {
        };
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

    private String[] allowedOrigins() {
        return allowedOrigins.toArray(String[]::new);
    }
}
