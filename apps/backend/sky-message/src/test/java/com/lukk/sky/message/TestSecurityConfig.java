package com.lukk.sky.message;

import com.lukk.sky.common.test.StubJwtDecoderFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return StubJwtDecoderFactory.emailAsTokenDecoder();
    }
}
