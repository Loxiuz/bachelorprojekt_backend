package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.StatusReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatusReadingRepository extends JpaRepository<StatusReading, UUID> {
}
