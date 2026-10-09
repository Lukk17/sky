package com.lukk.sky.gateway.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class StubOidcProvider implements AutoCloseable {

    private static final String REALM_PATH = "/realms/sky";
    private static final String DISCOVERY_PATH = REALM_PATH + "/.well-known/openid-configuration";

    private final HttpServer server;
    private final String issuerUri;

    private StubOidcProvider(HttpServer server, String issuerUri) {
        this.server = server;
        this.issuerUri = issuerUri;
    }

    static StubOidcProvider start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            String issuerUri = "http://127.0.0.1:" + server.getAddress().getPort() + REALM_PATH;
            server.createContext("/", exchange -> handle(exchange, issuerUri));
            server.start();

            return new StubOidcProvider(server, issuerUri);

        } catch (IOException e) {
            throw new UncheckedIOException("stub OIDC provider could not bind a local port", e);
        }
    }

    String issuerUri() {
        return issuerUri;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void handle(HttpExchange exchange, String issuerUri) throws IOException {
        if (!DISCOVERY_PATH.equals(exchange.getRequestURI().getPath())) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        byte[] body = discoveryDocument(issuerUri).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static String discoveryDocument(String issuerUri) {
        return """
                {
                  "issuer": "%1$s",
                  "authorization_endpoint": "%1$s/protocol/openid-connect/auth",
                  "token_endpoint": "%1$s/protocol/openid-connect/token",
                  "userinfo_endpoint": "%1$s/protocol/openid-connect/userinfo",
                  "jwks_uri": "%1$s/protocol/openid-connect/certs",
                  "end_session_endpoint": "%1$s/protocol/openid-connect/logout",
                  "response_types_supported": ["code"],
                  "subject_types_supported": ["public"],
                  "id_token_signing_alg_values_supported": ["RS256"],
                  "grant_types_supported": ["authorization_code", "refresh_token"],
                  "scopes_supported": ["openid", "email", "profile"],
                  "token_endpoint_auth_methods_supported": ["client_secret_basic", "client_secret_post"]
                }
                """.formatted(issuerUri);
    }
}
