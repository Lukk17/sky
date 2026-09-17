package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The gateway runs on Netty with no servlet stack, so the shared servlet exception handling must be absent
 * rather than merely unused. A servlet advice or resolver reaching a reactive context fails at startup.
 */
@DisplayName("sky-gateway carries no servlet exception handling")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class ServletExceptionHandlingAbsentTest {

    private static final String LAST_RESORT_RESOLVER = "com.lukk.sky.common.web.UnhandledExceptionResolver";
    private static final String SHARED_ADVICE = "com.lukk.sky.common.web.SkyRestExceptionHandler";
    private static final String SERVLET_RESOLVER_API = "org.springframework.web.servlet.HandlerExceptionResolver";

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("springWebMvcIsNotOnTheClasspath_soTheSharedAutoConfigurationNeverLoads")
    void springWebMvcIsNotOnTheClasspath_soTheSharedAutoConfigurationNeverLoads() {
        assertThatThrownBy(() -> Class.forName(SERVLET_RESOLVER_API))
                .as("adding spring-boot-starter-web here would also put Tomcat on the gateway")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    @DisplayName("neitherTheSharedAdviceNorTheLastResortResolverIsABeanHere")
    void neitherTheSharedAdviceNorTheLastResortResolverIsABeanHere() {
        assertThat(context.getBeanDefinitionNames())
                .as("a reactive context must hold neither")
                .noneMatch(name -> name.contains("unhandledExceptionResolver"))
                .noneMatch(name -> name.contains("skyRestExceptionHandler"));

        assertThatThrownBy(() -> Class.forName(LAST_RESORT_RESOLVER))
                .isInstanceOf(NoClassDefFoundError.class);

        assertThatThrownBy(() -> Class.forName(SHARED_ADVICE))
                .isInstanceOf(NoClassDefFoundError.class);
    }
}
