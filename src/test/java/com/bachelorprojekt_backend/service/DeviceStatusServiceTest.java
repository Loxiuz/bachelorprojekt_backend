package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.StatusReading;
import com.bachelorprojekt_backend.repository.DeviceRepository;
import com.bachelorprojekt_backend.repository.StatusReadingRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceStatusServiceTest {
    private final DeviceRepository devices = mock(DeviceRepository.class);
    private final StatusReadingRepository readings = mock(StatusReadingRepository.class);
    private final DeviceStatusService service = new DeviceStatusService(devices, readings);
    private final UUID deviceId = UUID.randomUUID();
    private final Instant readAt = Instant.parse("2026-01-01T12:00:00Z");
    private final DeviceStatusDTO status = new DeviceStatusDTO(readAt, 75, true, false, true);

    @Test
    void savesOneCompleteSnapshot() {
        Device device = new Device();
        when(devices.findById(deviceId)).thenReturn(Optional.of(device));

        service.saveDeviceStatus(status, deviceId);

        var saved = org.mockito.ArgumentCaptor.forClass(StatusReading.class);
        verify(readings).save(saved.capture());
        StatusReading reading = saved.getValue();
        assertNotNull(reading.getId());
        assertSame(device, reading.getDevice());
        assertEquals(readAt, reading.getReadAt());
        assertEquals(75, reading.getState().getBatteryLevel());
        assertTrue(reading.getState().getLocked());
        assertFalse(reading.getState().getDoorOpen());
        assertTrue(reading.getState().getIsOnline());
    }

    @Test
    void rejectsMissingDevice() {
        when(devices.findById(deviceId)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveDeviceStatus(status, deviceId));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(readings, never()).save(any());
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
