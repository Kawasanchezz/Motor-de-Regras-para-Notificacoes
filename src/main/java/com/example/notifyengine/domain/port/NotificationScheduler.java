package com.example.notifyengine.domain.port;

import com.example.notifyengine.domain.model.Notification;

import java.time.Instant;

public interface NotificationScheduler {

    void schedule(Notification notification, Instant when);
}
