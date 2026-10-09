package com.lukk.sky.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakRealmRoleConverter")
class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    @DisplayName("convert_mapsRealmRolesToUpperCaseRoleAuthorities")
    void convert_mapsRealmRolesToUpperCaseRoleAuthorities() {
        // given
        Jwt jwt = jwtWithRealmAccess(Map.of("roles", List.of("user", "admin")));

        // when
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // then
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("convert_returnsNoAuthorities_whenRealmAccessClaimIsAbsent")
    void convert_returnsNoAuthorities_whenRealmAccessClaimIsAbsent() {
        // given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();

        // when / then
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("convert_returnsNoAuthorities_whenRealmAccessClaimIsEmpty")
    void convert_returnsNoAuthorities_whenRealmAccessClaimIsEmpty() {
        // given
        Jwt jwt = jwtWithRealmAccess(Map.of());

        // when / then
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("convert_ignoresNonStringRoles")
    void convert_ignoresNonStringRoles() {
        // given
        Jwt jwt = jwtWithRealmAccess(Map.of("roles", List.of("user", 42)));

        // when / then
        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("convert_returnsNoAuthorities_whenRealmAccessCarriesNoRolesKey")
    void convert_returnsNoAuthorities_whenRealmAccessCarriesNoRolesKey() {
        // given
        Jwt jwt = jwtWithRealmAccess(Map.of("not-roles", "user"));

        // when / then
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    @DisplayName("convert_returnsNoAuthorities_whenTheRolesClaimIsNotAList")
    void convert_returnsNoAuthorities_whenTheRolesClaimIsNotAList() {
        // given
        Jwt jwt = jwtWithRealmAccess(Map.of("roles", "admin"));

        // when / then
        assertThat(converter.convert(jwt))
                .as("a realm mapper emitting a bare string must grant nothing, not ROLE_ADMIN")
                .isEmpty();
    }

    private Jwt jwtWithRealmAccess(Map<String, Object> realmAccess) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim("realm_access", realmAccess)
                .build();
    }
}
