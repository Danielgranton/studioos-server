package com.studioos.server.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "mpesa")
@Data
public class MpesaProperties {
    private String consumerKey;
    private String consumerSecret;
    private String passkey;
    private String shortcode;
    private String callbackUrl;
    private String timeoutUrl;
    private String environment; // "sandbox" or "production"
    private String initiatorName;
    private String securityCredential;
}