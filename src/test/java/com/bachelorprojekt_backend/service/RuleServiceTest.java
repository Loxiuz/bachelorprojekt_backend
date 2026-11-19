package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.alarm.HighActivityAlarm;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.model.Door;
import com.bachelorprojekt_backend.model.RuleConfig;
import com.bachelorprojekt_backend.model.Tenant;
import com.bachelorprojekt_backend.repository.DeviceEventRepository;
import com.bachelorprojekt_backend.repository.RuleConfigRepository;
import com.bachelorprojekt_backend.rule.HighActivityRule;
import com.bachelorprojekt_backend.types.AlarmType;
import com.bachelorprojekt_backend.types.DeviceType;
import com.bachelorprojekt_backend.types.DeviceEventType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuleServiceTest {
    private final RuleConfigRepository configs = mock(RuleConfigRepository.class);
    private final DeviceEventRepository openings = mock(DeviceEventRepository.class);
    private final AlarmService alarms = mock(AlarmService.class);
    private final RuleService rules = new RuleService(configs, openings, JsonMapper.builder().build(), alarms);
    private final UUID tenantId = UUID.randomUUID();
    private final UUID deviceId = UUID.randomUUID();
    private final UUID doorId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-01-01T12:00:00Z");
    private static final String CONFIG = """
            {"rules":[
              {"ruleName":"Høj aktivitet","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
               "ruleCondition":{"openings":5,"withinMinutes":10}}
            ]}
            """;

    private void config(UUID id, String json) {
        RuleConfig config = new RuleConfig();
        config.setConfig(json);
        when(configs.findById(id)).thenReturn(Optional.of(config));
    }

    private Device device(UUID tenant) {
        Tenant owner = new Tenant();
        owner.setId(tenant);
        Door door = new Door();
        door.setId(doorId);
        door.setTenant(owner);
        Device device = new Device();
        device.setId(deviceId);
        device.setDeviceType(DeviceType.DIGITAL_DOOR);
        device.setDoor(door);
        return device;
    }

    private DeviceEvent event(Device device, DeviceEventType type) {
        DeviceEvent event = new DeviceEvent();
        event.setDevice(device);
        event.setEventType(type);
        event.setOccurredAt(now);
        return event;
    }

    @Test
    void filtersRulesAndKeepsValidOnesWhenOthersAreInvalid() {
        config(tenantId, """
                {"rules":[
                  {"ruleName":"Ignored","ruleType":"OTHER"},
                  {"ruleName":"Other device","ruleType":"HIGH_ACTIVITY","deviceType":"OTHER",
                   "ruleCondition":{"openings":2,"withinMinutes":3}},
                  {"ruleName":"Invalid zero","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
                   "ruleCondition":{"openings":0,"withinMinutes":10}},
                  {"ruleName":"Invalid fractional","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
                   "ruleCondition":{"openings":2.5,"withinMinutes":10}},
                  {"ruleName":"Valid","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
                   "ruleCondition":{"openings":5,"withinMinutes":10}}
                ]}
                """);

        assertEquals(List.of(new HighActivityRule("Valid", 5, 10)),
                rules.getHighActivityRulesFromConfig(tenantId, DeviceType.DIGITAL_DOOR));
    }

    @Test
    void invalidOrMissingConfigurationDoesNotTriggerAlarm() {
        Device device = device(tenantId);
        when(configs.findById(tenantId)).thenReturn(Optional.empty());
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        config(tenantId, "{invalid");
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        config(tenantId, "{\"rules\":{}}");
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        verifyNoInteractions(openings, alarms);
    }

    @Test
    void evaluatesTenantAndDeviceSpecificRollingWindowAndThreshold() {
        config(tenantId, CONFIG);
        Device device = device(tenantId);
        when(openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                deviceId, DeviceEventType.DOOR_OPENED, now.minusSeconds(600), now)).thenReturn(3L, 4L, 5L, 4L);

        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));

        var captured = org.mockito.ArgumentCaptor.forClass(HighActivityAlarm.class);
        verify(alarms, times(2)).send(captured.capture());
        assertEquals(new HighActivityAlarm(AlarmType.HIGH_ACTIVITY, tenantId, deviceId, doorId,
                "Høj aktivitet", now, 5, 10), captured.getValue());
        verify(configs, times(4)).findById(tenantId);
        verify(openings, times(4)).countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                deviceId, DeviceEventType.DOOR_OPENED, now.minusSeconds(600), now);
    }

    @Test
    void tenantRulesAndDifferentThresholdsAreEvaluatedSeparately() {
        UUID otherTenant = UUID.randomUUID();
        config(tenantId, CONFIG);
        config(otherTenant, """
                {"rules":[
                  {"ruleName":"Two","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
                   "ruleCondition":{"openings":2,"withinMinutes":3}},
                  {"ruleName":"Three","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
                   "ruleCondition":{"openings":3,"withinMinutes":4}}
                ]}
                """);
        when(openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                deviceId, DeviceEventType.DOOR_OPENED, now.minusSeconds(180), now))
                .thenReturn(1L);
        when(openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                deviceId, DeviceEventType.DOOR_OPENED, now.minusSeconds(240), now))
                .thenReturn(2L);

        rules.evaluateEvent(event(device(otherTenant), DeviceEventType.DOOR_OPENED));
        var captured = org.mockito.ArgumentCaptor.forClass(HighActivityAlarm.class);
        verify(alarms, times(2)).send(captured.capture());
        assertEquals(List.of("Two", "Three"), captured.getAllValues().stream().map(HighActivityAlarm::ruleName).toList());
        assertTrue(captured.getAllValues().stream().allMatch(alarm -> otherTenant.equals(alarm.tenantId())));
        verify(configs, never()).findById(tenantId);
    }

    @Test
    void missingTenantIsLoggedWithoutAlarm() {
        Device device = new Device();
        device.setId(deviceId);
        rules.evaluateEvent(event(device, DeviceEventType.DOOR_OPENED));
        verifyNoInteractions(configs, openings, alarms);
    }

    @Test
    void unrelatedEventsDoNotEvaluateDoorOpeningRules() {
        rules.evaluateEvent(event(device(tenantId), DeviceEventType.LOCKED));
        rules.evaluateEvent(event(device(tenantId), DeviceEventType.UNLOCKED));
        verifyNoInteractions(configs, openings, alarms);
    }
}
