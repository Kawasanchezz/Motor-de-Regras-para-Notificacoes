package com.example.notifyengine.infra.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {

    List<RuleEntity> findByEventTypeAndActiveTrueOrderByPriorityAsc(String eventType);

    Page<RuleEntity> findByEventType(String eventType, Pageable pageable);
}
