package com.lukk.sky.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ActiveProfileGuard")
class ActiveProfileGuardTest {

    @Test
    @DisplayName("rejectUnrecognisedProfiles_doesNotThrow_whenEveryActiveProfileIsRecognised")
    void rejectUnrecognisedProfiles_doesNotThrow_whenEveryActiveProfileIsRecognised() {
        // given
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local", "test"});
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        // when / then
        assertThatCode(guard::rejectUnrecognisedProfiles).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejectUnrecognisedProfiles_throws_whenAnActiveProfileIsUnrecognised")
    void rejectUnrecognisedProfiles_throws_whenAnActiveProfileIsUnrecognised() {
        // given
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        ActiveProfileGuard guard = new ActiveProfileGuard(environment);

        // when / then
        assertThatThrownBy(guard::rejectUnrecognisedProfiles)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("staging")
                .hasMessageContaining("security config")
                .hasMessageContaining("RECOGNISED_PROFILES");
    }
}
