package com.aulix.aulix_backend.security.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CurrentUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private String tenantSlug;
}
