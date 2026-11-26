package com.bachelorprojekt_backend.model;

import com.bachelorprojekt_backend.types.RuleType;
import com.bachelorprojekt_backend.types.Severity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "alarms")
public class Alarm {

    @Id
    private UUID id;
    @ManyToOne
    private Device device;
    @ManyToOne
    @JoinColumn(name = "device_event_id")
    private DeviceEvent deviceEvent;
    @CreationTimestamp
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    private RuleType type;
    @Enumerated(EnumType.STRING)
    private Severity severity;
    private boolean acknowledged;

}
