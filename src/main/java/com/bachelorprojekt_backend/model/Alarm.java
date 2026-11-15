package com.bachelorprojekt_backend.model;

import com.bachelorprojekt_backend.types.AlarmType;
import com.bachelorprojekt_backend.types.Severity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    @CreationTimestamp
    private Instant createdAt;
    private AlarmType type;
    private Severity severity;
    private boolean acknowledged;

}
