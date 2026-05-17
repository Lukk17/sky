package com.lukk.sky.notify.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
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
}
