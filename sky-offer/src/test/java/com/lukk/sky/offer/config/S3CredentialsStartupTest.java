package com.lukk.sky.offer.config;

import com.lukk.sky.common.config.MissingCredentialException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("sky-offer object store credentials")
class S3CredentialsStartupTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(S3Config.class);

    @Test
    @DisplayName("run_whenBothCredentialsAreSupplied_startsTheContext")
    void run_whenBothCredentialsAreSupplied_startsTheContext() {
        runner.withPropertyValues("S3_ACCESS_KEY=sky", "S3_SECRET_KEY=s3cret")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(S3Client.class));
    }

    @Test
    @DisplayName("run_whenTheSecretKeyIsAbsent_failsStartupNamingTheVariableAndTheProperty")
    void run_whenTheSecretKeyIsAbsent_failsStartupNamingTheVariableAndTheProperty() {
        runner.withPropertyValues("S3_ACCESS_KEY=sky")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(MissingCredentialException.class)
                        .hasMessageContaining("S3_SECRET_KEY")
                        .hasMessageContaining("sky.s3.secret-key"));
    }

    @Test
    @DisplayName("run_whenNeitherCredentialIsSupplied_namesBothVariables")
    void run_whenNeitherCredentialIsSupplied_namesBothVariables() {
        runner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .isInstanceOf(MissingCredentialException.class)
                .hasMessageContaining("S3_ACCESS_KEY")
                .hasMessageContaining("S3_SECRET_KEY"));
    }

    @Test
    @DisplayName("run_whenTheLocalProfileIsActiveAndNoVariablesAreSet_startsWithoutObjecting")
    void run_whenTheLocalProfileIsActiveAndNoVariablesAreSet_startsWithoutObjecting() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
