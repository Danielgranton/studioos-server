package com.studioos.server.beatmarketplace.dto;

import lombok.Data;

@Data
public class MediaJobCallbackRequest {
    private String externalJobId;
    private boolean success;
    private String resultReference;   // populated only when success = true
    private String errorMessage;      // populated only when success = false
}