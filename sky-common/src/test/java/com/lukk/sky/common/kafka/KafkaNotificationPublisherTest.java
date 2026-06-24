package com.lukk.sky.common.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("KafkaNotificationPublisher")
@ExtendWith(MockitoExtension.class)
class KafkaNotificationPublisherTest {

    private static final String TOPIC = "test-topic";

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    KafkaNotificationPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaNotificationPublisher(kafkaTemplate, TOPIC);
    }

    @Test
    @DisplayName("sends the payload as JSON to the configured topic")
    void publish_sendsJsonToCorrectTopic() {
        KafkaPayloadModel payload = new KafkaPayloadModel("hello", "2024-01-01T00:00:00", "user@example.com");

        publisher.publish(payload);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());

        assertThat(topicCaptor.getValue())
                .as("message must be sent to the configured topic")
                .isEqualTo(TOPIC);
        assertThat(messageCaptor.getValue())
                .as("message must be serialised to JSON containing the payload field")
                .contains("\"payload\":\"hello\"")
                .contains("\"userInfo\":\"user@example.com\"");
    }

    @Test
    @DisplayName("sends a second message independently — publisher is stateless between calls")
    void publish_isStateless_betweenCalls() {
        KafkaPayloadModel first = new KafkaPayloadModel("first", "2024-01-01T00:00:00", "a@b.com");
        KafkaPayloadModel second = new KafkaPayloadModel("second", "2024-01-02T00:00:00", "c@d.com");

        publisher.publish(first);
        publisher.publish(second);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, org.mockito.Mockito.times(2)).send(org.mockito.Mockito.eq(TOPIC), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues().get(0)).contains("\"payload\":\"first\"");
        assertThat(messageCaptor.getAllValues().get(1)).contains("\"payload\":\"second\"");
    }
}
