package com.lukk.sky.notify.adapters.inbound;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.common.web.CorrelationId;
import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import com.lukk.sky.notify.domain.service.NotificationTransmissionServicePrimary;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the correlation id sky-common stamps onto a Kafka record on publish reaches the
 * logging context of the consumer, so producer and consumer log lines share one trace.
 */
@DisplayName("Kafka correlation id propagation: full-stack integration test")
@Import(TestSecurityConfig.class)
class KafkaCorrelationIdIntegrationTest extends AbstractIntegrationTest {

    private static final String CORRELATION_ID = "corr-notify-integration";
    private static final String USER_WITH_CORRELATION_ID = "alice@test.com";
    private static final String USER_WITHOUT_CORRELATION_ID = "bob@test.com";

    @Autowired
    private KafkaTemplate<String, String> dltKafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final RoutingLogRecorder recorder = new RoutingLogRecorder();

    @BeforeEach
    void attachRecorder() {
        recorder.start();
        routingLogger().addAppender(recorder);
    }

    @AfterEach
    void detachRecorder() {
        routingLogger().detachAppender(recorder);
        recorder.stop();
    }

    @Test
    @DisplayName("a record header puts the correlation id in the consumer logging context and clears it afterwards")
    void listener_whenRecordCarriesCorrelationHeader_thenConsumerLogsUnderTheSameCorrelationId()
            throws InterruptedException {
        // given
        send(USER_WITH_CORRELATION_ID, CORRELATION_ID);
        send(USER_WITHOUT_CORRELATION_ID, null);

        // when
        boolean bothHandled = recorder.awaitRoutingOf(USER_WITH_CORRELATION_ID, USER_WITHOUT_CORRELATION_ID);

        // then
        assertThat(bothHandled).isTrue();
        assertThat(recorder.correlationIdFor(USER_WITH_CORRELATION_ID)).isEqualTo(CORRELATION_ID);
        assertThat(recorder.correlationIdFor(USER_WITHOUT_CORRELATION_ID)).isNull();
    }

    private void send(String userInfo, String correlationId) {
        String message = objectMapper.writeValueAsString(
                new KafkaPayloadModel("offer-data", LocalDateTime.now().toString(), userInfo));
        ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_OFFER_TOPIC, message);

        if (correlationId != null) {
            record.headers().add(CorrelationId.HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }

        dltKafkaTemplate.send(record);
    }

    private Logger routingLogger() {
        return (Logger) LoggerFactory.getLogger(NotificationTransmissionServicePrimary.class);
    }

    /**
     * Captures the correlation id present in the logging context at the moment the domain service
     * logs the routing decision for a given user.
     */
    private static final class RoutingLogRecorder extends AppenderBase<ILoggingEvent> {

        private static final int AWAIT_SECONDS = 60;

        private final Map<String, String> correlationIdsByUser = new ConcurrentHashMap<>();
        private final Map<String, CountDownLatch> latchesByUser = new ConcurrentHashMap<>();

        @Override
        protected void append(ILoggingEvent event) {
            String message = event.getFormattedMessage();
            latchesByUser.forEach((user, latch) -> {
                if (message.contains(user)) {
                    correlationIdsByUser.putIfAbsent(user, String.valueOf(
                            event.getMDCPropertyMap().get(CorrelationId.MDC_KEY)));
                    latch.countDown();
                }
            });
        }

        private boolean awaitRoutingOf(String... users) throws InterruptedException {
            for (String user : users) {
                if (!latchesByUser.get(user).await(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                    return false;
                }
            }

            return true;
        }

        private String correlationIdFor(String user) {
            String captured = correlationIdsByUser.get(user);

            return "null".equals(captured) ? null : captured;
        }

        @Override
        public void start() {
            latchesByUser.put(USER_WITH_CORRELATION_ID, new CountDownLatch(1));
            latchesByUser.put(USER_WITHOUT_CORRELATION_ID, new CountDownLatch(1));
            super.start();
        }
    }
}
