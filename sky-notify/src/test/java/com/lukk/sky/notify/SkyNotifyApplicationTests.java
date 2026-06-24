package com.lukk.sky.notify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

@DisplayName("SkyNotify application context smoke test")
@Import(TestSecurityConfig.class)
class SkyNotifyApplicationTests extends AbstractIntegrationTest {

    @Test
    @DisplayName("Application context loads successfully on startup")
    void contextLoads_whenApplicationStarts_thenContextLoadsSuccessfully() {
    }
}
