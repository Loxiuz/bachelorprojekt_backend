package com.bachelorprojekt_backend.controller;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.service.DeviceStatusService;
import com.bachelorprojekt_backend.security.DevicePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceStatusController {
    private final DeviceStatusService deviceStatusService;

    public DeviceStatusController(DeviceStatusService deviceStatusService) {
        this.deviceStatusService = deviceStatusService;
    }

    @PostMapping("/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDeviceStatus(@Valid @RequestBody DeviceStatusDTO status, Authentication authentication) {
        DevicePrincipal device = (DevicePrincipal) authentication.getPrincipal();
        assert device != null;
        deviceStatusService.saveDeviceStatus(status, device.id());
    }
}
