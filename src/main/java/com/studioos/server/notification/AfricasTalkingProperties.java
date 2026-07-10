package com.studioos.server.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "africastalking")
public class AfricasTalkingProperties {
    private String username;
    private String apikey;
    private String senderId;
}