package com.studioos.server.communication;

public interface SmsProvider {

    String send(String recipient, String message);
}
