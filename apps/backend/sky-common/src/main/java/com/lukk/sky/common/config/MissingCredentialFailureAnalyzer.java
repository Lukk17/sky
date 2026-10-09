package com.lukk.sky.common.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class MissingCredentialFailureAnalyzer extends AbstractFailureAnalyzer<MissingCredentialException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MissingCredentialException cause) {
        return new FailureAnalysis(cause.getDescription(), cause.getAction(), cause);
    }
}
