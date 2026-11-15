package com.bachelorprojekt_backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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
    @Column(unique = true, length = 64)
    private String credentialHash;
}
