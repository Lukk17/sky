package com.lukk.sky.booking.domain.ports.api;

public interface RequestUriStrategy {
    String createRestUrl(String endpoint);
}
