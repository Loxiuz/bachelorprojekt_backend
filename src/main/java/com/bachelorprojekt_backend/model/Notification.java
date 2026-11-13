package com.bachelorprojekt_backend.model;

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
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;
    @ManyToOne
    private Alarm alarm;
    @ManyToOne
    private User recipient;
    @CreationTimestamp
    private Instant createdAt;
    private Instant sentAt;

}
