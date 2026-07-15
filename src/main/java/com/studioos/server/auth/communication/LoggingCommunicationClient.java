package com.studioos.server.auth.communication;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
public class LoggingCommunicationClient implements CommunicationClient {

    @Override
    public void send(CommunicationRequest request) {
        log.info(
                "Communication queued: type={} email={} phone={} subject={}",
                request.type(),
                request.email(),
                request.phone(),
                request.subject());
    }
}
