package com.lukk.sky.common.test;

import com.lukk.sky.common.security.KeycloakRealmRoleConverter;
import com.lukk.sky.common.security.SecurityAutoConfiguration.SkySecurityCustomizer;
import com.lukk.sky.common.security.SecurityPaths;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Test-only security wiring shared by every servlet module.
 *
 * <p>It mirrors the production chain: probes and API docs are open, a service's
 * {@link SkySecurityCustomizer} rules are applied, everything else needs a JWT, and method security
 * stays enabled with class proxies. The only production bean it replaces is the {@link JwtDecoder},
 * so tests never reach a real identity provider.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
public class SharedTestSecurityConfig {

    private static final String TOKEN_SUBJECT = "test-user";
    private static final String EMAIL_CLAIM = "email";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final List<String> TEST_ROLES = List.of("user", "admin");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+$");
    private static final SkySecurityCustomizer NO_ADDITIONAL_RULES = () -> auth -> {
    };

    @Bean
    SecurityFilterChain sharedTestSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<SkySecurityCustomizer> skySecurityCustomizer) throws Exception {
        SkySecurityCustomizer customizer = skySecurityCustomizer.getIfAvailable(() -> NO_ADDITIONAL_RULES);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(SecurityPaths.probesAndApiDocs().toArray(String[]::new)).permitAll();
                    customizer.additionalRules().customize(auth);
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Primary
    JwtDecoder sharedTestJwtDecoder() {
        return token -> {
            if (token == null || token.isBlank()) {
                throw new JwtException("test-profile JwtDecoder: missing token");
            }

            Jwt.Builder builder = Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim(REALM_ACCESS_CLAIM, Map.of(ROLES_CLAIM, TEST_ROLES));

            String email = decodeEmail(token);

            if (email == null) {
                builder.subject(TOKEN_SUBJECT);
            } else {
                builder.subject(email).claim(EMAIL_CLAIM, email);
            }

            return builder.build();
        };
    }

    @Bean
    JwtAuthenticationConverter sharedTestJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        return converter;
    }

    public static JwtRequestPostProcessor userJwt() {
        return jwt().jwt(builder -> builder.claim(EMAIL_CLAIM, TestUsers.TEST_USER_EMAIL))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static JwtRequestPostProcessor adminJwt() {
        return jwt().jwt(builder -> builder.claim(EMAIL_CLAIM, TestUsers.TEST_USER_EMAIL))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static String decodeEmail(String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);

            if (!isPrintableAscii(decoded)) {
                return null;
            }

            String value = new String(decoded, StandardCharsets.US_ASCII);

            return EMAIL_PATTERN.matcher(value).matches() ? value : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isPrintableAscii(byte[] bytes) {
        for (byte value : bytes) {
            if (value < 0x20 || value > 0x7E) {
                return false;
            }
        }

        return true;
    }
}
