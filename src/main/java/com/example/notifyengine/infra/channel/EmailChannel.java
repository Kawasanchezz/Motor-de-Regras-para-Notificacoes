package com.example.notifyengine.infra.channel;

import com.example.notifyengine.domain.model.Channel;
import org.springframework.stereotype.Component;

@Component
public class EmailChannel extends LoggingNotificationChannel {

    public EmailChannel() {
        super(Channel.EMAIL);
    }
}
