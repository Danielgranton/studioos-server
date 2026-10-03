package com.studioos.server.servicecatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomServiceRequest(
        @NotBlank @Size(max = 140) String name,
        @NotBlank @Size(max = 80) String category,
        @Size(max = 500) String description) {
}
