package com.bachelorprojekt_backend.model;

import com.bachelorprojekt_backend.types.DeviceEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_events", indexes = @Index(name = "idx_device_events_device_type_time", columnList = "device_id, event_type, occurred_at"))
public class DeviceEvent {
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private DeviceEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
