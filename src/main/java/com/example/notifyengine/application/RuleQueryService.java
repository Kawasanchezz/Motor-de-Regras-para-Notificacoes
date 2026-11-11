package com.example.notifyengine.application;

import com.example.notifyengine.domain.model.Rule;
import com.example.notifyengine.infra.persistence.RuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Carrega regras ativas do banco já convertidas para o modelo de domínio (usado pelo cache). */
@Service
public class RuleQueryService {

    private final RuleRepository repository;

    public RuleQueryService(RuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Rule> loadActive(String eventType) {
        return repository.findByEventTypeAndActiveTrueOrderByPriorityAsc(eventType).stream()
                .map(RuleMapper::toDomain)
                .toList();
    }
}
