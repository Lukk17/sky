package com.lukk.sky.booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DisplayName("SkyBookingApplication context tests")
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"offerTopic-1"})
@Import(TestSecurityConfig.class)
class SkyBookingApplicationTests {

    @Test
    @DisplayName("Spring application context loads without errors")
    void contextLoads_whenApplicationStarts_thenContextLoadsSuccessfully() {
    }

}
