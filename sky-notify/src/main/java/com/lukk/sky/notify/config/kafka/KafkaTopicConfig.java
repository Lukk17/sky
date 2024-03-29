package com.lukk.sky.notify.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;


@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic offerTopic() {
        return TopicBuilder.name(KAFKA_OFFER_TOPIC).build();
    }

    @Bean
    public NewTopic bookingTopic() {
        return TopicBuilder.name(KAFKA_BOOKING_TOPIC).build();
    }
}
