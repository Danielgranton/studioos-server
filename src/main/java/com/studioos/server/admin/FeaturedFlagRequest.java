package com.studioos.server.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeaturedFlagRequest {
    @NotNull
    private Boolean featured;
}
