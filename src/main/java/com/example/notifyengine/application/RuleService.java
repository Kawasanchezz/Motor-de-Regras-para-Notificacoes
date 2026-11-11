package com.example.notifyengine.application;

import com.example.notifyengine.api.dto.RuleRequest;
import com.example.notifyengine.api.dto.RuleResponse;
import com.example.notifyengine.application.exception.ResourceNotFoundException;
import com.example.notifyengine.domain.engine.ConditionFactory;
import com.example.notifyengine.domain.engine.constraint.ConstraintChain;
import com.example.notifyengine.domain.model.ConstraintSpec;
import com.example.notifyengine.infra.persistence.RuleEntity;
import com.example.notifyengine.infra.persistence.RuleRepository;
import com.example.notifyengine.infra.redis.RuleCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RuleService {

    private final RuleRepository repository;
    private final ConditionFactory conditionFactory;
    private final ConstraintChain constraintChain;
    private final RuleCache cache;

    public RuleService(RuleRepository repository,
                       ConditionFactory conditionFactory,
                       ConstraintChain constraintChain,
                       RuleCache cache) {
        this.repository = repository;
        this.conditionFactory = conditionFactory;
        this.constraintChain = constraintChain;
        this.cache = cache;
    }

    @Transactional
    public RuleResponse create(RuleRequest request) {
        validate(request);
        RuleEntity entity = new RuleEntity();
        RuleMapper.apply(entity, request);
        RuleEntity saved = repository.save(entity);
        invalidateAfterCommit(saved.getEventType());
        return RuleMapper.toResponse(saved);
    }

    @Transactional
    public RuleResponse update(UUID id, RuleRequest request) {
        validate(request);
        RuleEntity entity = find(id);
        String previousEventType = entity.getEventType();
        RuleMapper.apply(entity, request);
        RuleEntity saved = repository.saveAndFlush(entity);
        invalidateAfterCommit(previousEventType);
        invalidateAfterCommit(saved.getEventType());
        return RuleMapper.toResponse(saved);
    }

    @Transactional
    public RuleResponse setActive(UUID id, boolean active) {
        RuleEntity entity = find(id);
        entity.setActive(active);
        RuleEntity saved = repository.saveAndFlush(entity);
        invalidateAfterCommit(saved.getEventType());
        return RuleMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        RuleEntity entity = find(id);
        repository.delete(entity);
        invalidateAfterCommit(entity.getEventType());
    }

    @Transactional(readOnly = true)
    public RuleResponse get(UUID id) {
        return RuleMapper.toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<RuleResponse> list(String eventType, Pageable pageable) {
        Page<RuleEntity> page = (eventType == null || eventType.isBlank())
                ? repository.findAll(pageable)
                : repository.findByEventType(eventType, pageable);
        return page.map(RuleMapper::toResponse);
    }

    private RuleEntity find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regra não encontrada: " + id));
    }

    /** Valida a estrutura da condição e os parâmetros das restrições antes de persistir. */
    private void validate(RuleRequest request) {
        conditionFactory.build(request.condition());
        if (request.constraints() != null) {
            request.constraints().forEach(c ->
                    constraintChain.validate(new ConstraintSpec(c.type(), c.params())));
        }
    }

    /** Invalida o cache só depois do commit, para não repopulá-lo com dados antigos. */
    private void invalidateAfterCommit(String eventType) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cache.invalidate(eventType);
                }
            });
        } else {
            cache.invalidate(eventType);
        }
    }
}
