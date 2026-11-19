package com.bachelorprojekt_backend.alarm;

import com.bachelorprojekt_backend.types.AlarmType;

import java.time.Instant;
import java.util.UUID;

public record HighActivityAlarm(
        AlarmType type,
        UUID tenantId,
        UUID deviceId,
        UUID doorId,
        String ruleName,
        Instant occurredAt,
        long openings,
        int withinMinutes) {
}
