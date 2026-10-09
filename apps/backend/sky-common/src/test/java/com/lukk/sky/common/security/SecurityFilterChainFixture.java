package com.lukk.sky.common.security;

import jakarta.servlet.Filter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

final class SecurityFilterChainFixture implements AutoCloseable {

    private final AnnotationConfigWebApplicationContext context;
    private final MockMvc mvc;

    SecurityFilterChainFixture(Class<?> securityConfiguration) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(securityConfiguration);
        context.refresh();

        Filter springSecurityFilterChain = context.getBean("springSecurityFilterChain", Filter.class);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    MockMvc mvc() {
        return mvc;
    }

    @Override
    public void close() {
        context.close();
    }
}
