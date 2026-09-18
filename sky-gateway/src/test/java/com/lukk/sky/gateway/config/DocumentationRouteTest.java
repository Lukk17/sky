package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class DocumentationRouteTest {

    private static final StubUpstream UPSTREAM = StubUpstream.start();

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    @DynamicPropertySource
    static void upstreamUris(DynamicPropertyRegistry registry) {
        registry.add("sky-gateway.booking-uri", UPSTREAM::baseUrl);
        registry.add("sky-gateway.offer-uri", UPSTREAM::baseUrl);
        registry.add("sky-gateway.message-uri", UPSTREAM::baseUrl);
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.close();
    }

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "/booking/swagger-ui/index.html,           /swagger-ui/index.html,            /booking",
            "/booking/v3/api-docs,                     /v3/api-docs,                      /booking",
            "/booking/v3/api-docs/swagger-config,      /v3/api-docs/swagger-config,       /booking",
            "/offer/swagger-ui/index.html,             /swagger-ui/index.html,            /offer",
            "/offer/swagger-ui/swagger-initializer.js, /swagger-ui/swagger-initializer.js,/offer",
            "/offer/swagger-ui/swagger-ui-bundle.js,   /swagger-ui/swagger-ui-bundle.js,  /offer",
            "/offer/swagger-ui/swagger-ui.css,         /swagger-ui/swagger-ui.css,        /offer",
            "/offer/v3/api-docs,                       /v3/api-docs,                      /offer",
            "/offer/v3/api-docs/swagger-config,        /v3/api-docs/swagger-config,       /offer",
            "/offer/v3/api-docs/public,                /v3/api-docs/public,               /offer",
            "/msg/swagger-ui/index.html,               /swagger-ui/index.html,            /msg",
            "/msg/v3/api-docs,                         /v3/api-docs,                      /msg",
            "/msg/v3/api-docs/swagger-config,          /v3/api-docs/swagger-config,       /msg"
    })
    void documentationPath_whenProxied_thenArrivesUnprefixedAndCarriesTheStrippedPrefix(
            String publishedPath, String upstreamPath, String forwardedPrefix) {

        client.get().uri(publishedPath)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(echo -> assertThat(echo).isEqualTo(upstreamPath + " " + forwardedPrefix));
    }
}
