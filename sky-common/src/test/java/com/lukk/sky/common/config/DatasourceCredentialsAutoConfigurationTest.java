package com.lukk.sky.common.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DatasourceCredentialsAutoConfiguration")
class DatasourceCredentialsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DatasourceCredentialsAutoConfiguration.class));

    @Test
    @DisplayName("run_whenTheCredentialsAreResolved_startsTheContext")
    void run_whenTheCredentialsAreResolved_startsTheContext() {
        runner.withUserConfiguration(ResolvedCredentials.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(DatasourceCredentialsValidator.class));
    }

    @Test
    @DisplayName("run_whenThePasswordIsStillAPlaceholder_failsStartupNamingTheVariable")
    void run_whenThePasswordIsStillAPlaceholder_failsStartupNamingTheVariable() {
        runner.withUserConfiguration(UnresolvedPassword.class)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(MissingCredentialException.class)
                        .hasMessageContaining("POSTGRES_PASSWORD")
                        .hasMessageContaining("spring.datasource.password"));
    }

    @Test
    @DisplayName("run_whenNoConnectionDetailsBeanExists_registersTheValidatorButNeverFires")
    void run_whenNoConnectionDetailsBeanExists_registersTheValidatorButNeverFires() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(DatasourceCredentialsValidator.class));
    }

    @Test
    @DisplayName("run_whenJdbcIsNotOnTheClasspath_doesNotRegisterTheValidator")
    void run_whenJdbcIsNotOnTheClasspath_doesNotRegisterTheValidator() {
        runner.withClassLoader(new FilteredClassLoader(JdbcConnectionDetails.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(DatasourceCredentialsValidator.class));
    }

    @Test
    @DisplayName("run_whenTheDetailsBeanOnlyExposesCredentialsAfterInitialization_doesNotReadThemTooEarly")
    void run_whenTheDetailsBeanOnlyExposesCredentialsAfterInitialization_doesNotReadThemTooEarly() {
        runner.withUserConfiguration(LateBindingCredentials.class)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class ResolvedCredentials {

        @Bean
        JdbcConnectionDetails jdbcConnectionDetails() {
            return new StubJdbcConnectionDetails("sky", "s3cret");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UnresolvedPassword {

        @Bean
        JdbcConnectionDetails jdbcConnectionDetails() {
            return new StubJdbcConnectionDetails("sky", "${POSTGRES_PASSWORD}");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LateBindingCredentials {

        @Bean
        JdbcConnectionDetails jdbcConnectionDetails() {
            return new ContainerBackedJdbcConnectionDetails();
        }
    }

    private static final class ContainerBackedJdbcConnectionDetails implements JdbcConnectionDetails, InitializingBean {

        private @Nullable String credentials;

        @Override
        public void afterPropertiesSet() {
            credentials = "sky";
        }

        @Override
        public String getUsername() {
            return require();
        }

        @Override
        public String getPassword() {
            return require();
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:postgresql://localhost:5432/sky";
        }

        private String require() {
            if (credentials == null) {
                throw new IllegalStateException(
                        "Container cannot be obtained before the connection details bean has been initialized");
            }

            return credentials;
        }
    }

    private record StubJdbcConnectionDetails(@Nullable String username, @Nullable String password)
            implements JdbcConnectionDetails {

        @Override
        public @Nullable String getUsername() {
            return username;
        }

        @Override
        public @Nullable String getPassword() {
            return password;
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:postgresql://localhost:5432/sky";
        }
    }
}
