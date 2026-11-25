package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.TenantAlarmRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantAlarmRuleConfigRepository extends JpaRepository<TenantAlarmRuleConfig, UUID> {
}
