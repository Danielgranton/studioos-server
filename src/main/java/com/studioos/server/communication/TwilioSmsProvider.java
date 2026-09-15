package com.studioos.server.communication;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TwilioSmsProvider implements SmsProvider {

    private final TwilioProperties properties;

    @PostConstruct
    void initialize() {
        if (!properties.isEnabled()) {
            return;
        }

        if (!StringUtils.hasText(properties.getAccountSid())
                || !StringUtils.hasText(properties.getAuthToken())
                || !StringUtils.hasText(properties.getFromNumber())) {
            throw new IllegalStateException(
                    "Twilio is enabled but TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, or TWILIO_FROM_NUMBER is missing"
            );
        }

        Twilio.init(properties.getAccountSid(), properties.getAuthToken());
    }

    @Override
    public String send(String recipient, String message) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Twilio SMS provider is disabled");
        }

        return Message.creator(
                        new PhoneNumber(recipient),
                        new PhoneNumber(properties.getFromNumber()),
                        message
                )
                .create()
                .getSid();
    }
}
