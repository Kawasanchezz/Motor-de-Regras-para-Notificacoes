package com.example.notifyengine.api.dto;

import com.example.notifyengine.domain.model.Channel;

public record PreferenceResponse(String userId, Channel channel, String eventType, boolean optedOut) {
}
