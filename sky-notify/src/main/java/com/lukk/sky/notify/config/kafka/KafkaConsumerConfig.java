package com.lukk.sky.notify.config.kafka;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * sky-notify consumer wiring.
 *
 * <p>Reliability posture:
 * <ul>
 *   <li>{@code enable.auto.commit=false} + {@code AckMode.MANUAL_IMMEDIATE} —
 *       listeners ack only after the WebSocket emission succeeds.</li>
 *   <li>{@code DefaultErrorHandler} with {@link FixedBackOff} (3 retries, 1s
 *       apart) and a {@link DeadLetterPublishingRecoverer} routing failures to
 *       {@code <topic>.DLT}.</li>
 *   <li>Non-retryable exceptions ({@link com.google.gson.JsonSyntaxException},
 *       {@link com.google.gson.JsonParseException},
 *       {@link IllegalArgumentException}) route to DLT on first failure rather
 *       than burning the retry budget on a poison pill. The notify path
 *       deserializes with Gson, so Gson's parse exceptions are the poison-pill
 *       signal.</li>
 *   <li>{@code auto.offset.reset=earliest} so a restart doesn't silently skip
 *       events produced while sky-notify was down.</li>
 * </ul>
 *
 * <p>DLT topics ({@code bookingTopic-1.DLT}, {@code offerTopic-1.DLT}) must
 * exist in the broker; the Kafka Helm chart provisions them.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value("${sky.kafka.consumer.concurrency:1}")
    private int concurrency;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    /**
     * DLT publisher uses a dedicated producer (acks=all, idempotent) so the
     * error path itself is durable.
     */
    @Bean
    public ProducerFactory<String, String> dltProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> dltKafkaTemplate(ProducerFactory<String, String> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
        handler.addNotRetryableExceptions(
                JsonSyntaxException.class,
                JsonParseException.class,
                IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(concurrency);
        return factory;
    }
}
