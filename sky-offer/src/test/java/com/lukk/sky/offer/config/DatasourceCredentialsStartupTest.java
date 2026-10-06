package com.lukk.sky.offer.config;

import com.lukk.sky.common.config.DatasourceCredentialsAutoConfiguration;
import com.lukk.sky.common.config.MissingCredentialException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("sky-offer datasource credentials")
class DatasourceCredentialsStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DatasourceCredentialsAutoConfiguration.class));

    @Test
    @DisplayName("run_whenBothCredentialsAreSupplied_startsTheContext")
    void run_whenBothCredentialsAreSupplied_startsTheContext() {
        runner.withPropertyValues("POSTGRES_USER=sky", "POSTGRES_PASSWORD=s3cret")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("run_whenPostgresPasswordIsAbsent_failsStartupNamingTheVariableAndTheProperty")
    void run_whenPostgresPasswordIsAbsent_failsStartupNamingTheVariableAndTheProperty() {
        runner.withPropertyValues("POSTGRES_USER=sky")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(MissingCredentialException.class)
                        .hasMessageContaining("POSTGRES_PASSWORD")
                        .hasMessageContaining("spring.datasource.password"));
    }

    @Test
    @DisplayName("run_whenNeitherCredentialIsSupplied_namesBothVariables")
    void run_whenNeitherCredentialIsSupplied_namesBothVariables() {
        runner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(MissingCredentialException.class)
                .hasMessageContaining("POSTGRES_USER")
                .hasMessageContaining("POSTGRES_PASSWORD"));
    }

    @Test
    @DisplayName("run_whenTheLocalProfileIsActiveAndNoVariablesAreSet_startsWithoutObjecting")
    void run_whenTheLocalProfileIsActiveAndNoVariablesAreSet_startsWithoutObjecting() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
