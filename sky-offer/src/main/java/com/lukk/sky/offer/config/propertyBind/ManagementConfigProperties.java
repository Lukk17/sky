package com.lukk.sky.offer.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "management")
@Getter
@RequiredArgsConstructor
public class ManagementConfigProperties {

    private final Endpoints endpoints;

    public record Endpoints(Web web) {
        public record Web(Exposure exposure) {
            public record Exposure(String include) {
            }
        }
    }


}
