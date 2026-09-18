package com.lukk.sky.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MissingCredentialFailureAnalyzer")
class MissingCredentialFailureAnalyzerTest {

    private final MissingCredentialFailureAnalyzer analyzer = new MissingCredentialFailureAnalyzer();

    @Test
    @DisplayName("analyze_whenTheFailureIsAMissingCredential_describesItAndSaysWhatToDo")
    void analyze_whenTheFailureIsAMissingCredential_describesItAndSaysWhatToDo() {
        MissingCredentialException failure = new MissingCredentialException(
                List.of(new MissingCredential("POSTGRES_PASSWORD", "spring.datasource.password")));

        FailureAnalysis analysis = analyzer.analyze(failure);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription())
                .contains("spring.datasource.password")
                .contains("${POSTGRES_PASSWORD}")
                .contains("POSTGRES_PASSWORD is not set");
        assertThat(analysis.getAction())
                .contains("POSTGRES_PASSWORD");
        assertThat(analysis.getCause()).isSameAs(failure);
    }

    @Test
    @DisplayName("analyze_whenTheFailureIsWrappedByTheContainer_stillFindsTheCause")
    void analyze_whenTheFailureIsWrappedByTheContainer_stillFindsTheCause() {
        MissingCredentialException cause = new MissingCredentialException(
                List.of(new MissingCredential("S3_SECRET_KEY", "sky.s3.secret-key")));

        FailureAnalysis analysis = analyzer.analyze(new IllegalStateException("wrapped", cause));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("sky.s3.secret-key");
    }

    @Test
    @DisplayName("analyze_whenTheFailureIsSomethingElse_returnsNull")
    void analyze_whenTheFailureIsSomethingElse_returnsNull() {
        assertThat(analyzer.analyze(new IllegalStateException("unrelated"))).isNull();
    }
}
