package com.lukk.sky.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String incoming = request.getHeader(CorrelationId.HEADER);
        String correlationId = StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();

        CorrelationId.set(correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);

        } finally {
            CorrelationId.clear();
        }
    }
}
