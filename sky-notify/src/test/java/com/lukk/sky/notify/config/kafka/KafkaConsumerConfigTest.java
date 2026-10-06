package com.lukk.sky.notify.config.kafka;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("KafkaConsumerConfig: retryable vs non-retryable failure classification")
class KafkaConsumerConfigTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> dltKafkaTemplate = mock(KafkaTemplate.class);

    @SuppressWarnings("unchecked")
    private final Consumer<String, String> consumer = mock(Consumer.class);

    private final MessageListenerContainer container = mock(MessageListenerContainer.class);

    private DefaultErrorHandler errorHandler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void createErrorHandler() {
        ProducerFactory<String, String> producerFactory = mock(ProducerFactory.class);
        given(producerFactory.getConfigurationProperties()).willReturn(Map.of());
        given(dltKafkaTemplate.getProducerFactory()).willReturn(producerFactory);
        given(dltKafkaTemplate.send(any(ProducerRecord.class)))
                .willReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        errorHandler = new KafkaConsumerConfig().kafkaErrorHandler(dltKafkaTemplate);
    }

    @Test
    @DisplayName("a Jackson parsing failure is dead-lettered on the first attempt instead of being retried")
    void kafkaErrorHandler_whenJacksonFailure_thenRecoversToDeadLetterTopicWithoutRetrying() {
        // given
        ConsumerRecord<String, String> record = consumerRecord(0L, "{not-json");

        // when
        boolean recovered = errorHandler.handleOne(malformedJsonFailure(), record, consumer, container);

        // then
        assertThat(recovered).isTrue();

        ArgumentCaptor<ProducerRecord<String, String>> deadLettered = captorForProducerRecord();
        verify(dltKafkaTemplate).send(deadLettered.capture());
        assertThat(deadLettered.getValue().topic()).isEqualTo(KAFKA_OFFER_TOPIC + ".DLT");
    }

    @Test
    @DisplayName("an unexpected runtime failure is retried instead of dead-lettered")
    void kafkaErrorHandler_whenTransientFailure_thenRetriesInsteadOfRecovering() {
        // given
        ConsumerRecord<String, String> record = consumerRecord(1L, "{}");

        // when
        boolean recovered = errorHandler.handleOne(new IllegalStateException("broker hiccup"), record, consumer, container);

        // then
        assertThat(recovered).isFalse();
        verify(dltKafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    @Test
    @DisplayName("an invalid-argument failure is dead-lettered on the first attempt instead of being retried")
    void kafkaErrorHandler_whenIllegalArgument_thenRecoversWithoutRetrying() {
        // given
        ConsumerRecord<String, String> record = consumerRecord(2L, "{}");

        // when
        boolean recovered = errorHandler.handleOne(new IllegalArgumentException("bad header"), record, consumer, container);

        // then
        assertThat(recovered).isTrue();
        verify(dltKafkaTemplate).send(any(ProducerRecord.class));
    }

    private ConsumerRecord<String, String> consumerRecord(long offset, String value) {
        return new ConsumerRecord<>(KAFKA_OFFER_TOPIC, 0, offset, "key", value);
    }

    private JacksonException malformedJsonFailure() {
        try {
            JsonMapper.builder().build().readValue("{not-json", KafkaPayloadModel.class);
        } catch (JacksonException expected) {
            return expected;
        }

        throw new IllegalStateException("malformed JSON was accepted by the mapper");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<ProducerRecord<String, String>> captorForProducerRecord() {
        return ArgumentCaptor.forClass((Class<ProducerRecord<String, String>>) (Class<?>) ProducerRecord.class);
    }
}
