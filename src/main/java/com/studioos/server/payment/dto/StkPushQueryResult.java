package com.studioos.server.payment.dto;

public record StkPushQueryResult(Status status, String resultCode, String description) {
    public enum Status { SUCCESS, FAILED, PENDING, UNKNOWN }

    public static StkPushQueryResult success(String description) {
        return new StkPushQueryResult(Status.SUCCESS, "0", description);
    }

    public static StkPushQueryResult failed(String code, String description) {
        return new StkPushQueryResult(Status.FAILED, code, description);
    }

    public static StkPushQueryResult pending(String description) {
        return new StkPushQueryResult(Status.PENDING, null, description);
    }

    public static StkPushQueryResult unknown(String description) {
        return new StkPushQueryResult(Status.UNKNOWN, null, description);
    }
}
