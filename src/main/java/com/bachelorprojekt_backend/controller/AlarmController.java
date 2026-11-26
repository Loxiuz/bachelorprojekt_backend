package com.bachelorprojekt_backend.controller;

import com.bachelorprojekt_backend.dto.AlarmResponse;
import com.bachelorprojekt_backend.service.AlarmService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<AlarmResponse> getAlarms() {
        return alarmService.getAlarms();
    }

    @PostMapping("/{alarmId}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public void acknowledgeAlarm(@PathVariable UUID alarmId) {

        alarmService.acknowledgeAlarm(alarmId);
    }
}
