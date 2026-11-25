package com.bachelorprojekt_backend.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.model.Door;
import com.bachelorprojekt_backend.model.TenantAlarmRuleConfig;
import com.bachelorprojekt_backend.model.Tenant;
import com.bachelorprojekt_backend.repository.CurrentStatusReadingRepository;
import com.bachelorprojekt_backend.repository.DeviceRepository;
import com.bachelorprojekt_backend.repository.DeviceEventRepository;
import com.bachelorprojekt_backend.repository.TenantAlarmRuleConfigRepository;
import com.bachelorprojekt_backend.repository.StatusReadingRepository;
import com.bachelorprojekt_backend.types.DeviceType;
import com.bachelorprojekt_backend.types.DeviceEventType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=classpath:application.properties",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HighActivityPostgresTest {
    @Autowired private DeviceStatusService statuses;
    @Autowired private DeviceEventRepository openings;
    @Autowired private DeviceRepository devices;
    @Autowired private StatusReadingRepository readings;
    @Autowired private CurrentStatusReadingRepository currentReadings;
    @Autowired private TenantAlarmRuleConfigRepository configs;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AlarmService alarmService;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactions;
    private final Instant base = Instant.parse("2026-01-01T12:00:00Z");
    private final Logger logger = (Logger) LoggerFactory.getLogger(HighActivityPostgresTest.class);
    private final Logger alarmLogger = (Logger) LoggerFactory.getLogger(AlarmService.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void startLogging() {
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        alarmLogger.addAppender(appender);
    }

    @AfterEach
    void stopLogging() {
        logger.detachAppender(appender);
        alarmLogger.detachAppender(appender);
        appender.stop();
    }

    private UUID createDevice(int threshold) {
        UUID deviceId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> {
            Tenant tenant = new Tenant(UUID.randomUUID(), "Test tenant");
            entityManager.persist(tenant);
            Door door = new Door();
            door.setId(UUID.randomUUID());
            door.setTenant(tenant);
            entityManager.persist(door);
            Device device = new Device();
            device.setId(deviceId);
            device.setDoor(door);
            device.setDeviceType(DeviceType.DIGITAL_DOOR);
            entityManager.persist(device);
            TenantAlarmRuleConfig config = new TenantAlarmRuleConfig();
            config.setTenant(tenant);
            config.setRuleConfig("""
                    {"rules":[{"ruleName":"High activity","ruleType":"HIGH_ACTIVITY","deviceType":"DIGITAL_DOOR",
                      "ruleCondition":{"openings":%d,"withinMinutes":10}}]}
                    """.formatted(threshold));
            entityManager.persist(config);
        });
        return deviceId;
    }

    private void send(UUID deviceId, long seconds, boolean open) {
        statuses.saveDeviceStatus(new DeviceStatusDTO(base.plusSeconds(seconds), 75, true, open, true), deviceId);
    }

    @Test
    void persistsWindowSeparatesDevicesAndRearmsAtTheBoundary() {
        UUID device = createDevice(5);
        UUID other = createDevice(5);
        send(device, 0, false);
        send(other, 0, false);
        for (long second : new long[]{1, 61, 121, 181}) {
            send(device, second, true);
            send(device, second + 1, false);
        }
        send(other, 3, true);
        transactions.executeWithoutResult(status -> {
            DeviceEvent lockEvent = new DeviceEvent();
            lockEvent.setId(UUID.randomUUID());
            lockEvent.setDevice(entityManager.getReference(Device.class, device));
            lockEvent.setEventType(DeviceEventType.LOCKED);
            lockEvent.setOccurredAt(base.plusSeconds(200));
            entityManager.persist(lockEvent);
        });
        assertTrue(appender.list.isEmpty());

        RuleService recreatedRules = new RuleService(configs, openings, objectMapper, alarmService);
        DeviceStatusService recreatedStatusService = new DeviceStatusService(
                devices, readings, currentReadings, openings, recreatedRules);
        transactions.executeWithoutResult(status -> recreatedStatusService.saveDeviceStatus(
                new DeviceStatusDTO(base.plusSeconds(241), 75, true, true, true), device));
        assertEquals(1, appender.list.size());
        send(device, 242, false);
        send(device, 301, true);
        assertEquals(1, appender.list.size());
        send(device, 302, false);

        assertEquals(4, openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                device, DeviceEventType.DOOR_OPENED, base.plusSeconds(61), base.plusSeconds(661)));
        send(device, 662, true);
        assertEquals(2, appender.list.size());
        assertTrue(appender.list.stream().allMatch(event -> event.getFormattedMessage().contains(device.toString())));
        assertEquals(7, openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                device, DeviceEventType.DOOR_OPENED, base, base.plusSeconds(662)));
        assertEquals(1, openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                other, DeviceEventType.DOOR_OPENED, base, base.plusSeconds(662)));
    }

    @Test
    void concurrentDuplicateStatusesProduceOneOpeningAndAlarm() throws Exception {
        UUID device = createDevice(1);
        send(device, 0, false);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> tryOpen(device, start));
            var second = executor.submit(() -> tryOpen(device, start));
            start.countDown();
            var results = java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(code -> code == HttpStatus.NO_CONTENT).count());
            assertEquals(1, results.stream().filter(code -> code == HttpStatus.CONFLICT).count());
        }
        assertEquals(1, openings.countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
                device, DeviceEventType.DOOR_OPENED, base, base.plusSeconds(1)));
        assertEquals(1, appender.list.size());
    }

    private HttpStatus tryOpen(UUID device, CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            send(device, 1, true);
            return HttpStatus.NO_CONTENT;
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
            return HttpStatus.CONFLICT;
        }
    }
}
