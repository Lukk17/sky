package com.lukk.sky.common.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives a real {@code DispatcherServlet} to pin where the last-resort resolver sits in the resolution order.
 * A service advice declares no order, so it inherits {@link Ordered#LOWEST_PRECEDENCE} and no second advice can
 * sort behind it. The resolver is not an advice, which is what makes it genuinely last.
 */
@DisplayName("UnhandledExceptionResolver ordering over a real DispatcherServlet")
class UnhandledExceptionResolverOrderingTest {

    private final AnnotationConfigWebApplicationContext context = webContext();

    private final MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    @DisplayName("mappedException_isAnsweredByTheUnorderedServiceAdvice_notByTheResolver")
    void mappedException_isAnsweredByTheUnorderedServiceAdvice_notByTheResolver() throws Exception {
        mvc.perform(get("/mapped"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(ServiceAdvice.CONFLICT_DETAIL));
    }

    @Test
    @DisplayName("frameworkException_isAnsweredByTheSharedHandler_notByTheResolver")
    void frameworkException_isAnsweredByTheSharedHandler_notByTheResolver() throws Exception {
        mvc.perform(get("/mapped").accept(MediaType.APPLICATION_ATOM_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406))
                .andExpect(jsonPath("$.detail").value("Acceptable representations: [application/json]."));
    }

    @Test
    @DisplayName("unmappedException_fallsThroughToTheResolver_asAProblemDetail")
    void unmappedException_fallsThroughToTheResolver_asAProblemDetail() throws Exception {
        mvc.perform(get("/unmapped"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value(UnhandledExceptionResolver.DETAIL))
                .andExpect(jsonPath("$.instance").value("/unmapped"));
    }

    @Test
    @DisplayName("resolverSortsBehindEveryAdvice_becauseAnAdviceDefaultsToLowestPrecedence")
    void resolverSortsBehindEveryAdvice_becauseAnAdviceDefaultsToLowestPrecedence() {
        UnhandledExceptionResolver resolver = context.getBean(UnhandledExceptionResolver.class);

        assertThat(resolver.getOrder())
                .as("the composite holding every advice is registered at order 0")
                .isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    private static AnnotationConfigWebApplicationContext webContext() {
        AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
        webContext.setServletContext(new MockServletContext());
        webContext.register(WebConfiguration.class);
        webContext.refresh();

        return webContext;
    }

    @EnableWebMvc
    @Configuration(proxyBeanMethods = false)
    static class WebConfiguration {

        @Bean
        FailingController failingController() {
            return new FailingController();
        }

        @Bean
        ServiceAdvice serviceAdvice() {
            return new ServiceAdvice();
        }

        @Bean
        SkyRestExceptionHandler skyRestExceptionHandler() {
            return new SkyRestExceptionHandler();
        }

        @Bean
        UnhandledExceptionResolver unhandledExceptionResolver(
                ObjectProvider<RequestMappingHandlerAdapter> handlerAdapters) {

            return new UnhandledExceptionResolver(handlerAdapters);
        }
    }

    @RestController
    static class FailingController {

        @GetMapping(value = "/mapped", produces = MediaType.APPLICATION_JSON_VALUE)
        String mapped() {
            throw new MappedException();
        }

        @GetMapping("/unmapped")
        String unmapped() {
            throw new IllegalStateException("ownerEmail was null");
        }
    }

    @RestControllerAdvice
    static class ServiceAdvice {

        static final String CONFLICT_DETAIL = "Somebody else got there first.";

        @ExceptionHandler(MappedException.class)
        @ResponseStatus(HttpStatus.CONFLICT)
        ErrorResponse handleMapped(MappedException ex) {
            return ErrorResponse.builder(ex, HttpStatus.CONFLICT, CONFLICT_DETAIL).build();
        }
    }

    static final class MappedException extends RuntimeException {

        private MappedException() {
            super("a domain failure the service maps itself");
        }
    }
}
