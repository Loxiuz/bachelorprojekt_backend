package com.bachelorprojekt_backend.dto;

import java.util.UUID;

public record AlarmResponse(
        UUID id,
        UUID deviceId,
        UUID deviceEventId,
        String createdAt,
        String ruleType,
        boolean acknowledged //Samme som læst eller ulæst
) {
}
