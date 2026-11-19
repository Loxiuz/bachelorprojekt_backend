package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.alarm.HighActivityAlarm;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.model.RuleConfig;
import com.bachelorprojekt_backend.model.Tenant;
import com.bachelorprojekt_backend.repository.DeviceEventRepository;
import com.bachelorprojekt_backend.repository.RuleConfigRepository;
import com.bachelorprojekt_backend.rule.HighActivityRule;
import com.bachelorprojekt_backend.types.AlarmType;
import com.bachelorprojekt_backend.types.DeviceEventType;
import com.bachelorprojekt_backend.types.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RuleService {
    private final RuleConfigRepository ruleConfigRepository;
    private final DeviceEventRepository deviceEventRepository;
    private final ObjectMapper objectMapper;
    private final AlarmService alarmService;

    public RuleService(RuleConfigRepository ruleConfigRepository, DeviceEventRepository deviceEventRepository,
                       ObjectMapper objectMapper, AlarmService alarmService) {
        this.ruleConfigRepository = ruleConfigRepository;
        this.deviceEventRepository = deviceEventRepository;
        this.objectMapper = objectMapper;
        this.alarmService = alarmService;
    }

    public void evaluateEvent(DeviceEvent event) {
        if (event.getEventType() != DeviceEventType.DOOR_OPENED) {
            return;
        }
        Device device = event.getDevice();
        if (device.getDoor() == null || device.getDoor().getTenant() == null) {
            log.warn("Cannot evaluate high activity for device {}: tenant or door missing", device.getId());
            return;
        }
        Tenant tenant = device.getDoor().getTenant();
        Instant openedAt = event.getOccurredAt();
        for (HighActivityRule rule : getHighActivityRulesFromConfig(tenant.getId(), device.getDeviceType())) {
            Instant start = openedAt.minus(rule.withinMinutes(), ChronoUnit.MINUTES);
            long previousOpenings = deviceEventRepository.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                    device.getId(), DeviceEventType.DOOR_OPENED, start, openedAt);
            if (previousOpenings < rule.openings() && previousOpenings + 1 >= rule.openings()) {
                alarmService.send(new HighActivityAlarm(AlarmType.HIGH_ACTIVITY, tenant.getId(), device.getId(),
                        device.getDoor().getId(), rule.ruleName(), openedAt, previousOpenings + 1, rule.withinMinutes()));
            }
        }
    }

    public List<HighActivityRule> getHighActivityRulesFromConfig(UUID tenantId, DeviceType deviceType) {
        RuleConfig config = getConfig(tenantId);
        if (config == null) {
            return List.of();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(config.getConfig());
        } catch (JacksonException e) {
            log.warn("Invalid rule configuration JSON for tenant {}", tenantId, e);
            return List.of();
        }
        JsonNode rules = root == null ? null : root.get("rules");
        if (rules == null || !rules.isArray()) {
            log.warn("Invalid rule configuration for tenant {}: rules must be an array", tenantId);
            return List.of();
        }

        return result(rules, deviceType);
    }

    private RuleConfig getConfig(UUID tenantId) {
        RuleConfig config = ruleConfigRepository.findById(tenantId).orElse(null);
        if (config == null || config.getConfig() == null) {
            log.warn("No rule configuration for tenant {}", tenantId);
            return null;
        }
        return config;
    }

    private List<HighActivityRule> result(JsonNode rules, DeviceType deviceType) {
        List<HighActivityRule> result = new ArrayList<>();
        for (JsonNode rule : rules) {
            if (!"HIGH_ACTIVITY".equals(rule.path("ruleType").asString()) ||
                    !deviceType.name().equals(rule.path("deviceType").asString())) {
                continue;
            }
            JsonNode condition = rule.path("ruleCondition");
            if (positiveInt(condition.get("openings")) || positiveInt(condition.get("withinMinutes"))) {
                log.warn("Invalid high activity rule: openings and withinMinutes must be positive integers");
                continue;
            }
            result.add(new HighActivityRule(rule.path("ruleName").asString(),
                    condition.get("openings").intValue(), condition.get("withinMinutes").intValue()));
        }
        return result;
    }

    private boolean positiveInt(JsonNode value) {
        return value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() <= 0;
    }
}
