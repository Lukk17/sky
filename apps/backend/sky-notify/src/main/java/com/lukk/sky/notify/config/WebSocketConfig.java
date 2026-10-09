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
 * WebSocket broker over STOMP, JWT on CONNECT, per-user push via convertAndSendToUser.
 * This configurer runs at highest precedence so auth registers before security checks it.
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
