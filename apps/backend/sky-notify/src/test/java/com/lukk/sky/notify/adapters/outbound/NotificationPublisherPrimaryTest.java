package com.lukk.sky.notify.adapters.outbound;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.notify.adapters.outbound.websocket.WebSocketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Wire-format contract of the WebSocket payload, asserted against the Jackson mapper Spring Boot
 * auto-configures, which is the mapper the running service injects.
 */
@DisplayName("NotificationPublisherPrimary: WebSocket wire format")
class NotificationPublisherPrimaryTest {

    private static final String TARGET_USER = "user@test.com";
    private static final String PARTITION = "1";
    private static final String TOPIC = "sky.offer";
    private static final String GROUP_ID = "skyGroup";
    private static final String TIMESTAMP = "2201-06-20T16:35:47";
    private static final String OFFSET = "7";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    @Test
    @DisplayName("serializes payload and Kafka metadata into the WebSocket message")
    void publish_whenCalled_thenSendsSerializedEnvelopeToTheUserQueue() {
        contextRunner.run(context -> {
            // given
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            WebSocketService webSocketService = mock(WebSocketService.class);
            NotificationPublisherPrimary publisher = new NotificationPublisherPrimary(webSocketService, objectMapper);
            KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TIMESTAMP, TARGET_USER);

            // when
            publisher.publish(TARGET_USER, payload, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);

            // then
            String json = captureSentMessage(webSocketService);
            JsonNode envelope = objectMapper.readTree(json);
            assertThat(envelope.get("partition").asString()).isEqualTo(PARTITION);
            assertThat(envelope.get("topic").asString()).isEqualTo(TOPIC);
            assertThat(envelope.get("groupId").asString()).isEqualTo(GROUP_ID);
            assertThat(envelope.get("timestamp").asString()).isEqualTo(TIMESTAMP);
            assertThat(envelope.get("offset").asString()).isEqualTo(OFFSET);
            assertThat(envelope.get("kafkaPayloadModel").get("payload").asString()).isEqualTo("offer-data");
            assertThat(envelope.get("kafkaPayloadModel").get("accessedAt").asString()).isEqualTo(TIMESTAMP);
            assertThat(envelope.get("kafkaPayloadModel").get("userInfo").asString()).isEqualTo(TARGET_USER);
        });
    }

    @Test
    @DisplayName("omits null fields, keeping the format Gson produced")
    void publish_whenFieldIsNull_thenOmitsItFromTheJson() {
        contextRunner.run(context -> {
            // given
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            WebSocketService webSocketService = mock(WebSocketService.class);
            NotificationPublisherPrimary publisher = new NotificationPublisherPrimary(webSocketService, objectMapper);
            KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", null, TARGET_USER);

            // when
            publisher.publish(TARGET_USER, payload, PARTITION, TOPIC, null, TIMESTAMP, OFFSET);

            // then
            String json = captureSentMessage(webSocketService);
            assertThat(json).doesNotContain("groupId").doesNotContain("accessedAt").doesNotContain("null");
        });
    }

    @Test
    @DisplayName("keeps markup characters intact through a serialize-parse round trip")
    void publish_whenPayloadContainsMarkupCharacters_thenValueSurvivesTheRoundTrip() {
        contextRunner.run(context -> {
            // given
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            WebSocketService webSocketService = mock(WebSocketService.class);
            NotificationPublisherPrimary publisher = new NotificationPublisherPrimary(webSocketService, objectMapper);
            String markup = "<b>a & b</b> price=10 o'clock";
            KafkaPayloadModel payload = new KafkaPayloadModel(markup, TIMESTAMP, TARGET_USER);

            // when
            publisher.publish(TARGET_USER, payload, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);

            // then
            String json = captureSentMessage(webSocketService);
            JsonNode envelope = objectMapper.readTree(json);
            assertThat(envelope.get("kafkaPayloadModel").get("payload").asString()).isEqualTo(markup);
        });
    }

    @Test
    @DisplayName("tolerates an unknown property added by a producer instead of dead-lettering the event")
    void objectMapper_whenPayloadCarriesUnknownProperty_thenStillDeserializes() {
        contextRunner.run(context -> {
            // given
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
            String jsonWithNewProducerField =
                    "{\"payload\":\"offer-data\",\"accessedAt\":\"" + TIMESTAMP + "\",\"userInfo\":\"" + TARGET_USER
                            + "\",\"addedByANewerProducer\":\"value\"}";

            // when
            KafkaPayloadModel payload = objectMapper.readValue(jsonWithNewProducerField, KafkaPayloadModel.class);

            // then
            assertThat(payload.payload()).isEqualTo("offer-data");
            assertThat(payload.userInfo()).isEqualTo(TARGET_USER);
        });
    }

    private String captureSentMessage(WebSocketService webSocketService) {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(webSocketService).triggerMessage(eq(TARGET_USER), message.capture());

        return message.getValue();
    }
}
