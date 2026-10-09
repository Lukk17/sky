package com.lukk.sky.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("currentUserEmail_returnsEmailClaim_whenAuthenticatedWithJwtAuthenticationToken")
    void currentUserEmail_returnsEmailClaim_whenAuthenticatedWithJwtAuthenticationToken() {
        // given
        givenAuthentication(new JwtAuthenticationToken(jwtWithEmail("user@example.com")));

        // when
        String email = SecurityUtils.currentUserEmail();

        // then
        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("currentUserEmail_returnsEmailClaim_whenPrincipalIsAPlainJwt")
    void currentUserEmail_returnsEmailClaim_whenPrincipalIsAPlainJwt() {
        // given
        givenAuthentication(new TestingAuthenticationToken(jwtWithEmail("owner@example.com"), "credentials"));

        // when
        String email = SecurityUtils.currentUserEmail();

        // then
        assertThat(email).isEqualTo("owner@example.com");
    }

    @Test
    @DisplayName("currentUserEmail_throws_whenEmailClaimIsBlank")
    void currentUserEmail_throws_whenEmailClaimIsBlank() {
        // given
        givenAuthentication(new JwtAuthenticationToken(jwtWithEmail("   ")));

        // when / then
        assertThatThrownBy(SecurityUtils::currentUserEmail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not contain an 'email' claim");
    }

    @Test
    @DisplayName("currentUserEmail_throws_whenEmailClaimIsAbsent")
    void currentUserEmail_throws_whenEmailClaimIsAbsent() {
        // given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();
        givenAuthentication(new JwtAuthenticationToken(jwt));

        // when / then
        assertThatThrownBy(SecurityUtils::currentUserEmail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not contain an 'email' claim");
    }

    @Test
    @DisplayName("currentUserEmail_throws_whenThereIsNoJwtAuthentication")
    void currentUserEmail_throws_whenThereIsNoJwtAuthentication() {
        // given
        givenAuthentication(new TestingAuthenticationToken("plain-principal", "credentials"));

        // when / then
        assertThatThrownBy(SecurityUtils::currentUserEmail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No JWT authentication found");
    }

    @Test
    @DisplayName("currentUserEmail_throws_whenTheContextIsEmpty")
    void currentUserEmail_throws_whenTheContextIsEmpty() {
        // given
        SecurityContextHolder.clearContext();

        // when / then
        assertThatThrownBy(SecurityUtils::currentUserEmail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No JWT authentication found");
    }

    private void givenAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Jwt jwtWithEmail(String email) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim("email", email)
                .build();
    }
}
