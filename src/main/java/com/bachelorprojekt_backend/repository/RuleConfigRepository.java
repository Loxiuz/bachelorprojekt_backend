package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, UUID> {
}
