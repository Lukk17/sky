package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@DisplayName("The published OpenAPI document matches what the offer endpoints actually return")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import({com.lukk.sky.offer.TestSecurityConfig.class, com.lukk.sky.offer.TestcontainersConfiguration.class,
        com.lukk.sky.offer.TestS3Config.class})
class OfferApiDocumentTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OfferService offerService;

    private JsonNode document;

    @BeforeEach
    void fetchPublishedDocument() throws Exception {
        String body = mvc.perform(get("/v3/api-docs/public"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        document = objectMapper.readTree(body);
    }

    @Test
    @DisplayName("addOffer declares 201 and no 200, because it never returns 200")
    void addOffer_declaresOnly201() {
        JsonNode responses = responsesOf("addOffer");

        assertThat(responses.has("201")).isTrue();
        assertThat(responses.has("200"))
                .as("POST /owner/offers answers 201, so a blanket 200 would be a lie")
                .isFalse();
    }

    @Test
    @DisplayName("deleteOffer declares 204 and no 200, because it never returns 200")
    void deleteOffer_declaresOnly204() {
        JsonNode responses = responsesOf("deleteOffer");

        assertThat(responses.has("204")).isTrue();
        assertThat(responses.has("200"))
                .as("DELETE /owner/offers/{offerId} answers 204, so a blanket 200 would be a lie")
                .isFalse();
    }

    @Test
    @DisplayName("a 401 is declared without a body, because Spring Security answers with headers only")
    void unauthorized_isDeclaredWithoutContent() {
        JsonNode unauthorized = responsesOf("addOffer").get("401");

        assertThat(unauthorized).isNotNull();
        assertThat(unauthorized.has("content"))
                .as("the 401 body is empty, so no media type may be declared for it")
                .isFalse();
    }

    @Test
    @DisplayName("a 400 is declared as application/problem+json, which is what the handlers produce")
    void badRequest_isDeclaredAsProblemJson() {
        JsonNode badRequest = responsesOf("addOffer").get("400");

        assertThat(badRequest.get("content").has("application/problem+json")).isTrue();
    }

    @Test
    @DisplayName("every secured offer operation declares 403, because the role check can deny it")
    void securedOperations_declare403() {
        assertThat(responsesOf("addOffer").has("403")).isTrue();
        assertThat(responsesOf("edit").has("403")).isTrue();
        assertThat(responsesOf("deleteOffer").has("403")).isTrue();
        assertThat(responsesOf("getOwnedOffers").has("403")).isTrue();
        assertThat(responsesOf("uploadPhoto").has("403")).isTrue();
    }

    @Test
    @DisplayName("the public browse and search operations declare 401 but not 403, because an "
            + "unverifiable token reaches them too while no realm role is ever checked")
    void publicOperations_declare401ButNot403() {
        JsonNode browse = responsesOf("getAllOffers");
        JsonNode search = responsesOf("search");

        assertThat(browse.has("401")).as("browse declares %s", browse.propertyNames()).isTrue();
        assertThat(browse.has("403")).as("browse declares %s", browse.propertyNames()).isFalse();
        assertThat(search.has("401")).as("search declares %s", search.propertyNames()).isTrue();
        assertThat(search.has("403")).as("search declares %s", search.propertyNames()).isFalse();
    }

    @Test
    @DisplayName("the public browse and search operations stay anonymous, declaring an empty security list")
    void publicOperations_declareEmptySecurity() {
        assertThat(operationOf("getAllOffers").get("security")).isEmpty();
        assertThat(operationOf("search").get("security")).isEmpty();
    }

    @Test
    @DisplayName("every operation declares a 401 carrying the bearer challenge header")
    void everyOperation_declares401WithChallengeHeader() {
        for (JsonNode pathItem : document.get("paths")) {
            for (JsonNode operation : pathItem) {
                String operationId = operation.path("operationId").asString();
                JsonNode unauthorized = operation.get("responses").get("401");

                assertThat(unauthorized)
                        .as("%s declares no 401", operationId)
                        .isNotNull();
                assertThat(unauthorized.path("headers").has("WWW-Authenticate"))
                        .as("the 401 of %s carries no WWW-Authenticate header", operationId)
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("search states the blank and length rules in its operation description, which is the only "
            + "place a caller can read them")
    void search_describesWhatMakesATermInvalid() {
        String description = operationOf("search").path("description").asString();

        assertThat(description)
                .contains("must not be blank")
                .contains("100 characters")
                .contains("400");
    }

    @Test
    @DisplayName("the 400 on search keeps the shared description, because a class-level entry for a status "
            + "wins over a method-level one")
    void searchBadRequest_keepsTheSharedDescription() {
        JsonNode badRequest = responsesOf("search").get("400");

        assertThat(badRequest.path("description").asString())
                .isEqualTo("Bad Request: invalid input or a rejected domain rule");
    }

    @Test
    @DisplayName("getOfferOwner declares 401 but not 403, because it needs a token and no realm role")
    void getOfferOwner_declares401ButNot403() {
        JsonNode responses = responsesOf("getOfferOwner");

        assertThat(responses.has("401")).as("owner lookup declares %s", responses.propertyNames()).isTrue();
        assertThat(responses.has("403")).as("owner lookup declares %s", responses.propertyNames()).isFalse();
    }

    @Test
    @DisplayName("the published servers name every address that serves these paths, gateway first, "
            + "and never the port the documentation build binds")
    void servers_nameEveryAddressThesePathsAreServedOn() {
        JsonNode servers = document.get("servers");

        assertThat(servers).isNotNull();
        assertThat(fieldOf(servers, "url"))
                .as("each entry is a bare origin, because the published paths already carry /api/v1")
                .containsExactly(
                        "http://localhost:5777",
                        "https://skycloud.luksarna.com",
                        "http://localhost:5552");
        assertThat(fieldOf(servers, "description"))
                .containsExactly(
                        "Local, through the gateway, one origin for the whole stack",
                        "Production, through the ingress, behind oauth2-proxy",
                        "Local, straight at the service, bypassing the gateway");
        assertThat(servers.toString())
                .as("port 7972 exists only while the documentation build forks a boot, and "
                        + "\"Generated server url\" is what springdoc invents when the document declares none")
                .doesNotContain("7972")
                .doesNotContain("Generated server url");
    }

    private List<String> fieldOf(JsonNode servers, String field) {
        List<String> values = new ArrayList<>();

        for (JsonNode server : servers) {
            values.add(server.path(field).asString());
        }

        return values;
    }

    private JsonNode responsesOf(String operationId) {
        return operationOf(operationId).get("responses");
    }

    private JsonNode operationOf(String operationId) {
        for (JsonNode pathItem : document.get("paths")) {
            for (JsonNode operation : pathItem) {
                if (operationId.equals(operation.path("operationId").asString())) {
                    return operation;
                }
            }
        }

        return fail("operation %s is missing from the published document", operationId);
    }
}
