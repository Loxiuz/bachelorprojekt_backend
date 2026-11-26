package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.model.CurrentStatusReading;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.model.StatusReading;
import com.bachelorprojekt_backend.model.StatusReadingState;
import com.bachelorprojekt_backend.repository.CurrentStatusReadingRepository;
import com.bachelorprojekt_backend.repository.DeviceRepository;
import com.bachelorprojekt_backend.repository.DeviceEventRepository;
import com.bachelorprojekt_backend.repository.StatusReadingRepository;
import com.bachelorprojekt_backend.types.DeviceEventType;
import com.bachelorprojekt_backend.types.DeviceType;
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
    private final DeviceEventRepository deviceEventRepository;
    private final RuleService ruleService;

    public DeviceStatusService(DeviceRepository deviceRepository, StatusReadingRepository statusReadingRepository,
                               CurrentStatusReadingRepository currentStatusReadingRepository,
                               DeviceEventRepository deviceEventRepository, RuleService ruleService) {
        this.deviceRepository = deviceRepository;
        this.statusReadingRepository = statusReadingRepository;
        this.currentStatusReadingRepository = currentStatusReadingRepository;
        this.deviceEventRepository = deviceEventRepository;
        this.ruleService = ruleService;
    }

    @Transactional
    public void saveDeviceStatus(DeviceStatusDTO deviceStatusDTO, UUID deviceId) {
        Device device = deviceRepository.findByIdForStatusUpdate(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        CurrentStatusReading currentStatusReading = currentStatusReadingRepository.findByDevice(device);
        if (currentStatusReading != null) {
            if (!deviceStatusDTO.readAt().isAfter(currentStatusReading.getReadAt())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Status timestamp must be newer than the current status");
            }
            if (device.getDeviceType() == DeviceType.DIGITAL_DOOR
                    && Boolean.FALSE.equals(currentStatusReading.getState().getDoorOpen())
                    && Boolean.TRUE.equals(deviceStatusDTO.doorOpen())) {
                DeviceEvent event = new DeviceEvent();
                event.setId(UUID.randomUUID());
                event.setDevice(device);
                event.setEventType(DeviceEventType.DOOR_OPENED);
                event.setOccurredAt(deviceStatusDTO.readAt());
                deviceEventRepository.save(event);
                ruleService.evaluateEvent(event);
            }
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
