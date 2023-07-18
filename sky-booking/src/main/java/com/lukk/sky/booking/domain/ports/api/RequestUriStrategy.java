package com.lukk.sky.booking.domain.ports.api;

/**
 * Strategy interface for creating RESTful URLs.
 * This service provides operations for creating URLs based on given endpoint.
 */
public interface RequestUriStrategy {
    /**
     * Creates a RESTful URL based on the provided endpoint.
     *
     * @param endpoint The endpoint to be used in the URL.
     * @return The created URL.
     */
    String createRestUrl(String endpoint);
}
