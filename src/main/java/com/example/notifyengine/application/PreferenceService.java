package com.example.notifyengine.application;

import com.example.notifyengine.api.dto.PreferenceRequest;
import com.example.notifyengine.api.dto.PreferenceResponse;
import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.port.OptOutPort;
import com.example.notifyengine.infra.persistence.UserPreference;
import com.example.notifyengine.infra.persistence.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PreferenceService implements OptOutPort {

    private final UserPreferenceRepository repository;

    public PreferenceService(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOptedOut(String userId, Channel channel, String eventType) {
        return repository.isOptedOut(userId, channel, eventType);
    }

    @Transactional
    public List<PreferenceResponse> update(String userId, List<PreferenceRequest> requests) {
        for (PreferenceRequest request : requests) {
            String eventType = (request.eventType() == null || request.eventType().isBlank())
                    ? null : request.eventType();
            UserPreference preference = repository
                    .findByUserIdAndChannelAndEventType(userId, request.channel(), eventType)
                    .orElseGet(() -> {
                        UserPreference created = new UserPreference();
                        created.setUserId(userId);
                        created.setChannel(request.channel());
                        created.setEventType(eventType);
                        return created;
                    });
            preference.setOptedOut(request.optedOut());
            repository.save(preference);
        }
        return list(userId);
    }

    @Transactional(readOnly = true)
    public List<PreferenceResponse> list(String userId) {
        return repository.findByUserId(userId).stream()
                .map(p -> new PreferenceResponse(p.getUserId(), p.getChannel(), p.getEventType(), p.isOptedOut()))
                .toList();
    }
}
