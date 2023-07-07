package com.lukk.sky.booking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.lukk.sky.booking.config.Constants.KAFKA_TOPIC;


@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic bookingTopic() {
        return TopicBuilder.name(KAFKA_TOPIC).build();
    }
}
