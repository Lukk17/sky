package com.lukk.sky.notify.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging")
@Getter
@RequiredArgsConstructor
public class LoggingLvlConfigProperties {

    private final Level level;

    public record Level(Org org) {
        public record Org(Springframework springframework) {
            public record Springframework(String web) {
            }
        }
    }

}
