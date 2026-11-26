package com.bachelorprojekt_backend.alarm;

import com.bachelorprojekt_backend.types.RuleType;
import com.bachelorprojekt_backend.types.Severity;

import java.time.Instant;
import java.util.UUID;

public record HighActivityAlarm(
        RuleType type,
        UUID tenantId,
        UUID deviceId,
        UUID doorId,
        UUID deviceEventId,
        String ruleName,
        Severity severity,
        Instant occurredAt,
        long openings,
        int withinMinutes
 ) implements AlarmScenario {
    @Override
    public RuleType ruleType() {
        return type;
    }
    @Override
    public Severity alarmSeverity() {
        return severity;
    }
}
