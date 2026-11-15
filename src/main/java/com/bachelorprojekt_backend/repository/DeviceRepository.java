package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findByCredentialHash(String credentialHash);
}
