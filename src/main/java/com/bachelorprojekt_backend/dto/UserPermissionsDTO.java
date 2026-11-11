package com.bachelorprojekt_backend.dto;

import java.util.List;
import java.util.UUID;

public record UserPermissionsDTO(String role, String username, List<String> permissions, UUID tenantId) {
}
