package com.example.notifyengine.domain.port;

import com.example.notifyengine.domain.model.Channel;

public interface OptOutPort {

    boolean isOptedOut(String userId, Channel channel, String eventType);
}
