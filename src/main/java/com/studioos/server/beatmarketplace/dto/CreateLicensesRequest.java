package com.studioos.server.beatmarketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class CreateLicensesRequest {

    @NotEmpty
    @Valid
    private List<CreateLicenseRequest> licenses;
}