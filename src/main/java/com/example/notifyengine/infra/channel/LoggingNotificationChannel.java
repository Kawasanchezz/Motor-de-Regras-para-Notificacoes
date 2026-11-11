package com.example.notifyengine.infra.channel;

import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.port.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Canal simulado: apenas registra o envio no log. Substitua por integrações reais.
 * Por privacidade (dados pessoais), o destinatário é mascarado e o corpo da mensagem não é registrado.
 */
public abstract class LoggingNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationChannel.class);

    private final Channel type;

    protected LoggingNotificationChannel(Channel type) {
        this.type = type;
    }

    @Override
    public Channel type() {
        return type;
    }

    @Override
    public void send(Notification notification) {
        log.info("[{}] notificação {} enviada para '{}' (evento {}, {} caracteres)",
                type, notification.id(), mask(notification.recipient()), notification.eventType(),
                notification.message() == null ? 0 : notification.message().length());
    }

    /** Mantém só os 2 primeiros caracteres (ex.: "an***"). */
    static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, Math.min(2, value.length())) + "***";
    }
}
