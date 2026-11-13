package com.bachelorprojekt_backend.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "status_readings")
public class StatusReading {

    @Id
    private UUID id;
    @ManyToOne
    private Device device;
    private Instant readAt;
    @Embedded
    private StatusReadingState state;
}
