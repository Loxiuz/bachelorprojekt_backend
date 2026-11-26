package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.alarm.AlarmCreated;
import com.bachelorprojekt_backend.alarm.AlarmScenario;
import com.bachelorprojekt_backend.alarm.HighActivityAlarm;
import com.bachelorprojekt_backend.dto.AlarmResponse;
import com.bachelorprojekt_backend.exception_handler.NotFoundException;
import com.bachelorprojekt_backend.model.Alarm;
import com.bachelorprojekt_backend.model.DeviceEvent;
import com.bachelorprojekt_backend.repository.AlarmRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AlarmService {
    private final ApplicationEventPublisher events;
    private final AlarmRepository alarmRepository;

    public AlarmService(ApplicationEventPublisher events, AlarmRepository alarmRepository) {
        this.events = events;
        this.alarmRepository = alarmRepository;
    }

    public void send(HighActivityAlarm alarm) {
        events.publishEvent(alarm);
    }

    public void publishCreated(UUID alarmId) {
        events.publishEvent(new AlarmCreated(alarmId));
    }

    @TransactionalEventListener
    public void logCommittedAlarm(HighActivityAlarm alarm) {
        log.warn("Alarm type={} tenantId={} deviceId={} doorId={} rule={} occurredAt={} openings={} withinMinutes={}",
                alarm.type(), alarm.tenantId(), alarm.deviceId(), alarm.doorId(), alarm.ruleName(),
                alarm.occurredAt(), alarm.openings(), alarm.withinMinutes());
    }

    public List<AlarmResponse> getAlarms() {
        return alarmRepository.findAll().stream().map(this::toResponse).toList();
    }

    private AlarmResponse toResponse(Alarm alarm) {
        return new AlarmResponse(
                alarm.getId(),
                alarm.getDevice().getId(),
                alarm.getDeviceEvent() == null ? null : alarm.getDeviceEvent().getId(),
                alarm.getCreatedAt().toString(),
                alarm.getType().name(),
                alarm.isAcknowledged()
        );
    }

    public Alarm saveAlarm(DeviceEvent deviceEvent, AlarmScenario alarmScenario) {
        Alarm alarmEntity = new Alarm();
        alarmEntity.setId(UUID.randomUUID());
        alarmEntity.setDevice(deviceEvent.getDevice());
        alarmEntity.setDeviceEvent(deviceEvent);
        alarmEntity.setType(alarmScenario.ruleType());
        alarmEntity.setSeverity(alarmScenario.alarmSeverity());
        alarmEntity.setAcknowledged(false);

        return alarmRepository.save(alarmEntity);
    }

    public void acknowledgeAlarm(UUID alarmId) throws NotFoundException {
        Alarm alarm = alarmRepository.findById(alarmId).orElse(null);
        if(alarm == null ) {
            throw new NotFoundException("Alarm not found with id: " + alarmId);
        }
        alarm.setAcknowledged(true);
        alarmRepository.save(alarm);
    }
}
