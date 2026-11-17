package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.model.CurrentStatusReading;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.model.StatusReading;
import com.bachelorprojekt_backend.repository.CurrentStatusReadingRepository;
import com.bachelorprojekt_backend.repository.DeviceRepository;
import com.bachelorprojekt_backend.repository.DeviceEventRepository;
import com.bachelorprojekt_backend.repository.StatusReadingRepository;
import com.bachelorprojekt_backend.types.DeviceEventType;
import com.bachelorprojekt_backend.types.DeviceType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceStatusServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final StatusReadingRepository readings = mock(StatusReadingRepository.class);
    private final CurrentStatusReadingRepository currentStatusReadings = mock(CurrentStatusReadingRepository.class);
    private final DeviceEventRepository events = mock(DeviceEventRepository.class);
    private final RuleService rules = mock(RuleService.class);
    private final DeviceStatusService service = new DeviceStatusService(devices, readings, currentStatusReadings, events, rules);
    private final UUID deviceId = UUID.randomUUID();
    private final Instant readAt = Instant.parse("2026-01-01T12:00:00Z");
    private final DeviceStatusDTO status = new DeviceStatusDTO(readAt, 75, true, false, true);

    @Test
    void savesFirstSnapshotAsCurrentWithoutHistory() {
        Device device = new Device();
        when(devices.findByIdForStatusUpdate(deviceId)).thenReturn(Optional.of(device));

        service.saveDeviceStatus(status, deviceId);

        var saved = org.mockito.ArgumentCaptor.forClass(CurrentStatusReading.class);
        verify(currentStatusReadings).save(saved.capture());
        CurrentStatusReading reading = saved.getValue();
        var order = inOrder(devices, currentStatusReadings);
        order.verify(devices).findByIdForStatusUpdate(deviceId);
        order.verify(currentStatusReadings).findByDevice(device);
        order.verify(currentStatusReadings).save(reading);
        verifyNoInteractions(readings);
        assertNotNull(reading.getId());
        assertSame(device, reading.getDevice());
        assertEquals(readAt, reading.getReadAt());
        assertEquals(75, reading.getState().getBatteryLevel());
        assertTrue(reading.getState().getLocked());
        assertFalse(reading.getState().getDoorOpen());
        assertTrue(reading.getState().getIsOnline());
    }

    @Test
    void archivesEachPreviousSnapshotAndUpdatesTheSameCurrentRow() {
        Device device = new Device();
        when(devices.findByIdForStatusUpdate(deviceId)).thenReturn(Optional.of(device));
        when(currentStatusReadings.findByDevice(device)).thenReturn(null);

        service.saveDeviceStatus(status, deviceId);
        var current = org.mockito.ArgumentCaptor.forClass(CurrentStatusReading.class);
        verify(currentStatusReadings).save(current.capture());
        CurrentStatusReading currentReading = current.getValue();
        UUID currentId = currentReading.getId();
        when(currentStatusReadings.findByDevice(device)).thenReturn(currentReading);

        var second = new DeviceStatusDTO(readAt.plusSeconds(60), 50, false, true, false);
        var third = new DeviceStatusDTO(readAt.plusSeconds(120), 25, true, true, true);
        service.saveDeviceStatus(second, deviceId);
        service.saveDeviceStatus(third, deviceId);

        var archived = org.mockito.ArgumentCaptor.forClass(StatusReading.class);
        verify(readings, times(2)).save(archived.capture());
        List<StatusReading> history = archived.getAllValues();
        assertNotNull(history.get(0).getId());
        assertNotNull(history.get(1).getId());
        assertNotEquals(history.get(0).getId(), history.get(1).getId());
        assertSame(device, history.get(0).getDevice());
        assertSame(device, history.get(1).getDevice());
        assertEquals(readAt, history.get(0).getReadAt());
        assertEquals(75, history.get(0).getState().getBatteryLevel());
        assertTrue(history.getFirst().getState().getLocked());
        assertFalse(history.get(0).getState().getDoorOpen());
        assertTrue(history.get(0).getState().getIsOnline());
        assertEquals(readAt.plusSeconds(60), history.get(1).getReadAt());
        assertEquals(50, history.get(1).getState().getBatteryLevel());
        assertFalse(history.get(1).getState().getLocked());
        assertTrue(history.get(1).getState().getDoorOpen());
        assertFalse(history.get(1).getState().getIsOnline());
        verify(currentStatusReadings, times(3)).save(same(currentReading));
        assertEquals(currentId, currentReading.getId());
        assertEquals(third.readAt(), currentReading.getReadAt());
        assertEquals(25, currentReading.getState().getBatteryLevel());
    }

    @Test
    void rejectsMissingDevice() {
        when(devices.findByIdForStatusUpdate(deviceId)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveDeviceStatus(status, deviceId));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(readings, never()).save(any());
        verifyNoInteractions(currentStatusReadings);
    }

    @Test
    void recordsOnlyClosedToOpenTransitions() {
        Device device = new Device();
        device.setDeviceType(DeviceType.DIGITAL_DOOR);
        when(devices.findByIdForStatusUpdate(deviceId)).thenReturn(Optional.of(device));
        service.saveDeviceStatus(status, deviceId);
        var current = org.mockito.ArgumentCaptor.forClass(CurrentStatusReading.class);
        verify(currentStatusReadings).save(current.capture());
        when(currentStatusReadings.findByDevice(device)).thenReturn(current.getValue());

        service.saveDeviceStatus(new DeviceStatusDTO(readAt.plusSeconds(1), 75, true, true, true), deviceId);
        service.saveDeviceStatus(new DeviceStatusDTO(readAt.plusSeconds(2), 75, true, true, true), deviceId);

        var event = org.mockito.ArgumentCaptor.forClass(DeviceEvent.class);
        verify(events).save(event.capture());
        assertSame(device, event.getValue().getDevice());
        assertEquals(DeviceEventType.DOOR_OPENED, event.getValue().getEventType());
        assertEquals(readAt.plusSeconds(1), event.getValue().getOccurredAt());
        verify(rules).evaluateEvent(event.getValue());
    }

    @Test
    void firstOpenStatusDoesNotCountAsOpening() {
        Device device = new Device();
        device.setDeviceType(DeviceType.DIGITAL_DOOR);
        when(devices.findByIdForStatusUpdate(deviceId)).thenReturn(Optional.of(device));
        service.saveDeviceStatus(new DeviceStatusDTO(readAt, 75, true, true, true), deviceId);
        verifyNoInteractions(events, rules);
    }

    @Test
    void rejectsSameOrOlderTimestampWithoutChanges() {
        Device device = new Device();
        when(devices.findByIdForStatusUpdate(deviceId)).thenReturn(Optional.of(device));
        var current = new CurrentStatusReading();
        current.setReadAt(readAt);
        when(currentStatusReadings.findByDevice(device)).thenReturn(current);

        for (Instant time : List.of(readAt, readAt.minusSeconds(1))) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.saveDeviceStatus(new DeviceStatusDTO(time, 75, true, true, true), deviceId));
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        }
        verify(currentStatusReadings, never()).save(any());
        verifyNoInteractions(readings, events, rules);
        assertEquals(readAt, current.getReadAt());
    }

    @Test
    void validatesSnapshotFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(status).isEmpty());
            assertFalse(validator.validate(new DeviceStatusDTO(readAt, 101, true, false, true)).isEmpty());
            assertFalse(validator.validate(new DeviceStatusDTO(null, 0, true, false, null)).isEmpty());
        }
    }
}
