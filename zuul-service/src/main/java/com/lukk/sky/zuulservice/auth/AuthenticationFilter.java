package com.lukk.sky.zuulservice.auth;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;


@Component
@Slf4j
public class AuthenticationFilter extends ZuulFilter {

    @Value("${security.jwt.header:Authorization}")
    private String header;

    @Value("${security.jwt.secret:JwtSecretKey}")
    private String secret;

    @Value("${security.jwt.prefix:Bearer }")
    private String prefix;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return SIMPLE_HOST_ROUTING_FILTER_ORDER;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            String s = ctx.getRequest().getHeader(header);
            String token = s.replace(prefix, "");

            Claims claims = Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();

            ctx.addZuulRequestHeader("username", username);
        } catch (NullPointerException e) {
            log.info("User not logged.");
        }

        return null;
    }
}
