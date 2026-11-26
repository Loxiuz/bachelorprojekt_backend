package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlarmRepository extends JpaRepository<Alarm, UUID> {
}
