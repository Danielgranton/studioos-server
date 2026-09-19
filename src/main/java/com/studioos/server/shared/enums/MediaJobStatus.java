package com.studioos.server.shared.enums;

public enum MediaJobStatus {
    QUEUED,
    SUBMITTING,
    RUNNING,
    SUCCESS,
    FAILED,
    PENDING;

    public boolean isAwaitingProcessing() {
        return this == QUEUED || this == SUBMITTING || this == PENDING || this == RUNNING;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED;
    }
}
