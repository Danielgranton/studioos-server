package com.studioos.server.advertisement.dto;

import lombok.Data;

@Data
public class AdMediaJobCallbackRequest {
    private String externalJobId;
    private boolean success;
    private String resultReference;
    private String errorMessage;
}