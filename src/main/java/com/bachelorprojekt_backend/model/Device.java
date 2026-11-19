package com.bachelorprojekt_backend.model;

import com.bachelorprojekt_backend.types.DeviceType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "devices")
public class Device {
    @Id
    private UUID id;
    @ManyToOne
    private Door door;
    private String name;
    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;
    @Column(unique = true, length = 64)
    private String credentialHash;
}
