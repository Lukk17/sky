package com.lukk.sky.common.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.Collections;
import java.util.Map;

/**
 * Reads a JWT without verifying its signature, issuer, or expiry.
 *
 * <p>Only ever registered under the {@code local} Spring profile by {@link LocalSecurityAutoConfiguration}.
 */
public final class UnverifiedJwtDecoder implements JwtDecoder {

    private static final MappedJwtClaimSetConverter CLAIM_SET_CONVERTER =
            MappedJwtClaimSetConverter.withDefaults(Collections.emptyMap());

    @Override
    public Jwt decode(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadJwtException("local profile: no bearer token supplied");
        }

        try {
            JWT parsed = JWTParser.parse(token);
            Map<String, Object> headers = parsed.getHeader().toJSONObject();
            Map<String, Object> claims = CLAIM_SET_CONVERTER.convert(parsed.getJWTClaimsSet().getClaims());

            return Jwt.withTokenValue(token)
                    .headers(target -> target.putAll(headers))
                    .claims(target -> target.putAll(claims))
                    .build();

        } catch (ParseException ex) {
            throw new BadJwtException("local profile: token is not a well-formed JWT", ex);
        }
    }
}
