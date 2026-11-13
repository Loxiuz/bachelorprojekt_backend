package com.bachelorprojekt_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "doors")
public class Door {

    @Id
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    private String name;
    private String location;
}
