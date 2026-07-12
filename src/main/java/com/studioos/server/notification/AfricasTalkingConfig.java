package com.studioos.server.notification;

import com.africastalking.AfricasTalking;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AfricasTalkingConfig {
    private final AfricasTalkingProperties properties;

    public AfricasTalkingConfig(AfricasTalkingProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(properties.getUsername()) || !StringUtils.hasText(properties.getApikey())) {
            return;
        }

        AfricasTalking.initialize(properties.getUsername(), properties.getApikey());
    }
}
