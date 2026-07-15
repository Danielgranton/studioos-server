package com.studioos.server.session.dto;

import com.studioos.server.shared.enums.DeliverableType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSessionDeliverableRequest {
    @NotNull
    private DeliverableType type;

    @NotNull
    private String contentType;

    private Integer duration;
}
