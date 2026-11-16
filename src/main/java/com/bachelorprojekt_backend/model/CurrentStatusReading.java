package com.bachelorprojekt_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
@Getter
@Setter
@Entity
@Table(name = "current_status_readings")
public class CurrentStatusReading {

    @Id
    private UUID id;
    @ManyToOne
    @JoinColumn(nullable = false, unique = true)
    private Device device;
    private Instant readAt;
    @Embedded
    private StatusReadingState state;
}
