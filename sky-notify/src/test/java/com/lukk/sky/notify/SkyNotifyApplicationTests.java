package com.lukk.sky.notify;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class SkyNotifyApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
