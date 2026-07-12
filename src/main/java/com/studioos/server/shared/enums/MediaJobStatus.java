package com.studioos.server.shared.enums;

public enum MediaJobStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    PENDING;

    public boolean isAwaitingProcessing() {
        return this == QUEUED || this == PENDING || this == RUNNING;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
