package com.example.notifyengine.infra.channel;

import com.example.notifyengine.domain.model.Channel;
import org.springframework.stereotype.Component;

@Component
public class SmsChannel extends LoggingNotificationChannel {

    public SmsChannel() {
        super(Channel.SMS);
    }
}
