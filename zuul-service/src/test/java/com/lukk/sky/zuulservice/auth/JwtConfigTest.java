package com.lukk.sky.zuulservice.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JwtConfigTest {

    @Autowired
    JwtConfig jwtConfig;

    @Test
    public void getUri() {

        assertEquals("/login", jwtConfig.getUri());
    }

    @Test
    public void getHeader() {
        assertEquals("Authorization", jwtConfig.getHeader());

    }

    @Test
    public void getPrefix() {
        assertEquals("Bearer ", jwtConfig.getPrefix());

    }

    @Test
    public void getExpiration() {
        assertEquals(24 * 60 * 60, jwtConfig.getExpiration());

    }

    @Test
    public void getSecret() {
        assertEquals("JwtSecretKey", jwtConfig.getSecret());

    }
}
