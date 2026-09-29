package com.lukk.sky.notify.config;

import java.util.List;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class GatewayUserHandshakeInterceptor implements HandshakeInterceptor {

    public static final String GATEWAY_USER_HEADER = "X-Sky-User";
    public static final String GATEWAY_USER_ATTRIBUTE = "skyGatewayUser";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        List<String> values = request.getHeaders().get(GATEWAY_USER_HEADER);
        if (values != null && !values.isEmpty()) {
            String name = values.getFirst().trim();
            if (!name.isEmpty()) {
                attributes.put(GATEWAY_USER_ATTRIBUTE, name);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
