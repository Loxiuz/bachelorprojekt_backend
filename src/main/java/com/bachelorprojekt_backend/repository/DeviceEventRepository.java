package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.types.DeviceEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {
    long countByDeviceIdAndEventTypeAndOccurredAtAfterAndOccurredAtLessThanEqual(
            UUID deviceId, DeviceEventType eventType, Instant startExclusive, Instant endInclusive);
}
