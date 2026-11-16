package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.CurrentStatusReading;
import com.bachelorprojekt_backend.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CurrentStatusReadingRepository extends JpaRepository<CurrentStatusReading, UUID> {
    CurrentStatusReading findByDevice(Device device);
}
