package com.lukk.sky.notify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;

@ActiveProfiles("test")
@SpringBootTest(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {KAFKA_OFFER_TOPIC, KAFKA_BOOKING_TOPIC},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class SkyNotifyApplicationTests {


    @Test
    void contextLoads() {
    }

}
