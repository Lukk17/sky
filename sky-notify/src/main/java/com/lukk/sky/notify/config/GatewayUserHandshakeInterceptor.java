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
    /**
     * Fallback identity header set by oauth2-proxy on the nginx auth subrequest
     * ({@code X-Auth-Request-Email}) and forwarded to the upstream by the
     * notify ingress {@code auth-response-headers}. The ingress overwrites a
     * client-sent header of the same name with the validated session value, so
     * like {@link #GATEWAY_USER_HEADER} (which the gateway strips and resets)
     * it is edge-asserted, never caller-asserted.
     */
    public static final String EDGE_USER_HEADER = "X-Auth-Request-Email";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        List<String> values = request.getHeaders().get(GATEWAY_USER_HEADER);
        String name = firstNonBlank(values);
        if (name == null) {
            List<String> edgeValues = request.getHeaders().get(EDGE_USER_HEADER);
            name = firstNonBlank(edgeValues);
        }
        if (name != null) {
            attributes.put(GATEWAY_USER_ATTRIBUTE, name);
        }
        return true;
    }

    private static String firstNonBlank(List<String> values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
