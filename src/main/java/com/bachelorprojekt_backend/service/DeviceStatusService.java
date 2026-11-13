package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.StatusReading;
import com.bachelorprojekt_backend.model.StatusReadingState;
import com.bachelorprojekt_backend.repository.DeviceRepository;
import com.bachelorprojekt_backend.repository.StatusReadingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class DeviceStatusService {

    private final DeviceRepository deviceRepository;
    private final StatusReadingRepository statusReadingRepository;

    public DeviceStatusService(DeviceRepository deviceRepository, StatusReadingRepository statusReadingRepository) {
        this.deviceRepository = deviceRepository;
        this.statusReadingRepository = statusReadingRepository;
    }

    @Transactional
    public void saveDeviceStatus(DeviceStatusDTO deviceStatusDTO, UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        StatusReading statusReading = new StatusReading();
        statusReading.setId(UUID.randomUUID());
        statusReading.setDevice(device);
        statusReading.setReadAt(deviceStatusDTO.readAt());
        statusReading.setState(new StatusReadingState(deviceStatusDTO.batteryLevel(), deviceStatusDTO.locked(),
                deviceStatusDTO.doorOpen(), deviceStatusDTO.isOnline()));
        statusReadingRepository.save(statusReading);
    }
}
