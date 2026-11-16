package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.model.CurrentStatusReading;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.StatusReading;
import com.bachelorprojekt_backend.model.StatusReadingState;
import com.bachelorprojekt_backend.repository.CurrentStatusReadingRepository;
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
    private final CurrentStatusReadingRepository currentStatusReadingRepository;

    public DeviceStatusService(DeviceRepository deviceRepository, StatusReadingRepository statusReadingRepository, CurrentStatusReadingRepository currentStatusReadingRepository) {
        this.deviceRepository = deviceRepository;
        this.statusReadingRepository = statusReadingRepository;
        this.currentStatusReadingRepository = currentStatusReadingRepository;
    }

    @Transactional
    public void saveDeviceStatus(DeviceStatusDTO deviceStatusDTO, UUID deviceId) {
        Device device = deviceRepository.findByIdForStatusUpdate(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        CurrentStatusReading currentStatusReading = currentStatusReadingRepository.findByDevice(device);
        if (currentStatusReading != null) {
            statusReadingRepository.save(fromCurrentStatusReading(currentStatusReading));
        } else {
            currentStatusReading = new CurrentStatusReading();
            currentStatusReading.setId(UUID.randomUUID());
            currentStatusReading.setDevice(device);
        }

        currentStatusReading.setReadAt(deviceStatusDTO.readAt());
        currentStatusReading.setState(new StatusReadingState(deviceStatusDTO.batteryLevel(), deviceStatusDTO.locked(),
                deviceStatusDTO.doorOpen(), deviceStatusDTO.isOnline()));
        currentStatusReadingRepository.save(currentStatusReading);
    }

    public StatusReading fromCurrentStatusReading(CurrentStatusReading currentStatusReading) {
        StatusReading statusReading = new StatusReading();
        statusReading.setId(UUID.randomUUID());
        statusReading.setDevice(currentStatusReading.getDevice());
        statusReading.setReadAt(currentStatusReading.getReadAt());
        statusReading.setState(currentStatusReading.getState());
        return statusReading;
    }
}
