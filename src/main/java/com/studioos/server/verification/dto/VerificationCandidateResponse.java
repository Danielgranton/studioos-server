package com.studioos.server.verification.dto;

import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.VerificationStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VerificationCandidateResponse {
    String id;
    String name;
    String email;
    Role role;
    VerificationStatus verificationStatus;
    String reason;
}
