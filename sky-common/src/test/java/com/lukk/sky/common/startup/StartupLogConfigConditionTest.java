package com.lukk.sky.common.startup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StartupLogConfig classpath condition")
class StartupLogConfigConditionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StartupLogConfig.class));

    @Test
    @DisplayName("run_whenSpringWebIsOnTheClasspath_registersTheStartupLog")
    void run_whenSpringWebIsOnTheClasspath_registersTheStartupLog() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(StartupLogConfig.class));
    }

    @Test
    @DisplayName("run_whenRestClientIsNotOnTheClasspath_doesNotRegisterTheStartupLog")
    void run_whenRestClientIsNotOnTheClasspath_doesNotRegisterTheStartupLog() {
        runner.withClassLoader(new FilteredClassLoader(RestClient.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(StartupLogConfig.class));
    }

    @Test
    @DisplayName("run_whenTheRequestFactoryIsNotOnTheClasspath_doesNotRegisterTheStartupLog")
    void run_whenTheRequestFactoryIsNotOnTheClasspath_doesNotRegisterTheStartupLog() {
        runner.withClassLoader(new FilteredClassLoader(SimpleClientHttpRequestFactory.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(StartupLogConfig.class));
    }
}
