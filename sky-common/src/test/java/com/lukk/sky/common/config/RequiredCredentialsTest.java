package com.lukk.sky.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequiredCredentials")
class RequiredCredentialsTest {

    @Test
    @DisplayName("orFailStartup_whenEveryValueIsResolved_doesNotThrow")
    void orFailStartup_whenEveryValueIsResolved_doesNotThrow() {
        assertThatCode(() -> RequiredCredentials.check()
                .and("spring.datasource.username", "sky")
                .and("spring.datasource.password", "s3cret")
                .orFailStartup())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("orFailStartup_whenAValueIsStillAPlaceholder_namesTheVariableAndTheProperty")
    void orFailStartup_whenAValueIsStillAPlaceholder_namesTheVariableAndTheProperty() {
        assertThatThrownBy(() -> RequiredCredentials.check()
                .and("spring.datasource.username", "sky")
                .and("spring.datasource.password", "${POSTGRES_PASSWORD}")
                .orFailStartup())
                .isInstanceOf(MissingCredentialException.class)
                .hasMessageContaining("POSTGRES_PASSWORD")
                .hasMessageContaining("spring.datasource.password");
    }

    @Test
    @DisplayName("orFailStartup_whenSeveralValuesArePlaceholders_reportsAllOfThemInDeclarationOrder")
    void orFailStartup_whenSeveralValuesArePlaceholders_reportsAllOfThemInDeclarationOrder() {
        MissingCredentialException thrown = catchMissingCredential(() -> RequiredCredentials.check()
                .and("spring.datasource.username", "${POSTGRES_USER}")
                .and("spring.datasource.password", "${POSTGRES_PASSWORD}")
                .orFailStartup());

        assertThat(thrown.getMissing())
                .extracting(MissingCredential::environmentVariable)
                .containsExactly("POSTGRES_USER", "POSTGRES_PASSWORD");
        assertThat(thrown.getMissing())
                .extracting(MissingCredential::property)
                .containsExactly("spring.datasource.username", "spring.datasource.password");
    }

    @Test
    @DisplayName("orFailStartup_whenAValueIsNull_doesNotThrow")
    void orFailStartup_whenAValueIsNull_doesNotThrow() {
        assertThatCode(() -> RequiredCredentials.check()
                .and("spring.datasource.password", null)
                .orFailStartup())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("orFailStartup_whenAValueIsBlank_doesNotThrow")
    void orFailStartup_whenAValueIsBlank_doesNotThrow() {
        assertThatCode(() -> RequiredCredentials.check()
                .and("spring.datasource.password", "   ")
                .orFailStartup())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("orFailStartup_whenAValueOnlyContainsPlaceholderSyntax_doesNotThrow")
    void orFailStartup_whenAValueOnlyContainsPlaceholderSyntax_doesNotThrow() {
        assertThatCode(() -> RequiredCredentials.check()
                .and("spring.datasource.password", "pa${ss}word")
                .and("sky.s3.secret-key", "${unclosed")
                .and("sky.s3.access-key", "${TWO}${PLACEHOLDERS}")
                .orFailStartup())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("orFailStartup_whenAPlaceholderIsSurroundedByWhitespace_stillReportsIt")
    void orFailStartup_whenAPlaceholderIsSurroundedByWhitespace_stillReportsIt() {
        assertThatThrownBy(() -> RequiredCredentials.check()
                .and("sky.s3.access-key", "  ${S3_ACCESS_KEY}  ")
                .orFailStartup())
                .isInstanceOf(MissingCredentialException.class)
                .hasMessageContaining("S3_ACCESS_KEY");
    }

    @Test
    @DisplayName("getMessage_whenOneCredentialIsMissing_readsWithoutKnowledgeOfThisCodebase")
    void getMessage_whenOneCredentialIsMissing_readsWithoutKnowledgeOfThisCodebase() {
        MissingCredentialException thrown = catchMissingCredential(() -> RequiredCredentials.check()
                .and("spring.datasource.password", "${POSTGRES_PASSWORD}")
                .orFailStartup());

        assertThat(thrown.getMessage())
                .contains("1 required credential is not configured")
                .contains("spring.datasource.password is still the literal text \"${POSTGRES_PASSWORD}\"")
                .contains("environment variable POSTGRES_PASSWORD is not set")
                .contains("Set POSTGRES_PASSWORD in this service's environment, then start it again.");
    }

    @Test
    @DisplayName("getMessage_whenTwoCredentialsAreMissing_pluralisesTheCount")
    void getMessage_whenTwoCredentialsAreMissing_pluralisesTheCount() {
        MissingCredentialException thrown = catchMissingCredential(() -> RequiredCredentials.check()
                .and("spring.datasource.username", "${POSTGRES_USER}")
                .and("spring.datasource.password", "${POSTGRES_PASSWORD}")
                .orFailStartup());

        assertThat(thrown.getMessage())
                .contains("2 required credentials are not configured")
                .contains("Set POSTGRES_USER and POSTGRES_PASSWORD in this service's environment");
    }

    private static MissingCredentialException catchMissingCredential(Runnable check) {
        try {
            check.run();
        } catch (MissingCredentialException expected) {
            return expected;
        }

        throw new AssertionError("expected MissingCredentialException, nothing was thrown");
    }
}
