package com.lukk.sky.zuulservice.auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AuthenticationFilterTest {

    @Autowired
    AuthenticationFilter authenticationFilter;

    @Test
    public void filterType() {
        assertEquals(PRE_TYPE, authenticationFilter.filterType());
    }

    @Test
    public void filterOrder() {
        assertEquals(SIMPLE_HOST_ROUTING_FILTER_ORDER, authenticationFilter.filterOrder());
    }

    @Test
    public void shouldFilter() {
        assertTrue(authenticationFilter.shouldFilter());
    }

    @Test
    public void run() {
        assertNull(authenticationFilter.run());
    }
}
