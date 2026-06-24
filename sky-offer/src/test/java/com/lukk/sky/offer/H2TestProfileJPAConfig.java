package com.lukk.sky.offer;

import org.springframework.context.annotation.Configuration;

/**
 * Formerly provided an H2 datasource for the test profile. Replaced by
 * Testcontainers PostgreSQL wired via {@code @ServiceConnection} in
 * {@link AbstractIntegrationTest}. Kept as an empty placeholder so the
 * class name remains resolvable if any external tooling references it.
 */
@Configuration
public class H2TestProfileJPAConfig {
}
