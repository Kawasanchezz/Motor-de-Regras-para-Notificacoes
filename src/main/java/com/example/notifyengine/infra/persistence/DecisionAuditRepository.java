package com.example.notifyengine.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface DecisionAuditRepository
        extends JpaRepository<DecisionAudit, UUID>, JpaSpecificationExecutor<DecisionAudit> {
}
