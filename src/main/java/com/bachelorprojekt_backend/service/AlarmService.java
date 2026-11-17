package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.alarm.HighActivityAlarm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class AlarmService {
    private final ApplicationEventPublisher events;

    public AlarmService(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void send(HighActivityAlarm alarm) {
        events.publishEvent(alarm);
    }

    @TransactionalEventListener
    public void logCommittedAlarm(HighActivityAlarm alarm) {
        log.warn("Alarm type={} tenantId={} deviceId={} doorId={} rule={} occurredAt={} openings={} withinMinutes={}",
                alarm.type(), alarm.tenantId(), alarm.deviceId(), alarm.doorId(), alarm.ruleName(),
                alarm.occurredAt(), alarm.openings(), alarm.withinMinutes());
    }
}
