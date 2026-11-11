package com.example.notifyengine.infra.channel;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("notify.dispatch")
public record DispatchProperties(@DefaultValue("3") int maxAttempts,
                                 @DefaultValue("200") long initialBackoffMs,
                                 @DefaultValue("2.0") double multiplier) {
}
