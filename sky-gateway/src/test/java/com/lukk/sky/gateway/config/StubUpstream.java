package com.lukk.sky.gateway.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class StubUpstream implements AutoCloseable {

    static final String FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    private final HttpServer server;
    private final String baseUrl;

    private StubUpstream(HttpServer server, String baseUrl) {
        this.server = server;
        this.baseUrl = baseUrl;
    }

    static StubUpstream start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", StubUpstream::echoPathAndForwardedPrefix);
            server.start();

            return new StubUpstream(server, "http://127.0.0.1:" + server.getAddress().getPort());

        } catch (IOException e) {
            throw new UncheckedIOException("stub upstream could not bind a local port", e);
        }
    }

    String baseUrl() {
        return baseUrl;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void echoPathAndForwardedPrefix(HttpExchange exchange) throws IOException {
        String prefix = exchange.getRequestHeaders().getFirst(FORWARDED_PREFIX_HEADER);
        String echo = exchange.getRequestURI().getPath() + " " + prefix;
        byte[] body = echo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, body.length);

        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
