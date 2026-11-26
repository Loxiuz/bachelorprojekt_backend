package com.bachelorprojekt_backend.alarm;

import com.bachelorprojekt_backend.types.RuleType;
import com.bachelorprojekt_backend.types.Severity;

public interface AlarmScenario {
    RuleType ruleType();
    Severity alarmSeverity();
}
