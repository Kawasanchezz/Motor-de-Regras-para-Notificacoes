package com.example.notifyengine.infra.channel;

import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.port.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Despacha pelo canal correto com retry e backoff exponencial. */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Map<Channel, NotificationChannel> channels;
    private final DispatchProperties properties;

    public NotificationDispatcher(List<NotificationChannel> channels, DispatchProperties properties) {
        this.channels = channels.stream().collect(Collectors.toMap(NotificationChannel::type, Function.identity()));
        this.properties = properties;
    }

    /** @return true se enviou com sucesso em alguma das tentativas */
    public boolean dispatch(Notification notification) {
        NotificationChannel channel = channels.get(notification.channel());
        if (channel == null) {
            log.error("Nenhum canal registrado para {}", notification.channel());
            return false;
        }
        long backoff = properties.initialBackoffMs();
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                channel.send(notification);
                return true;
            } catch (Exception e) {
                log.warn("Falha ao enviar {} (tentativa {}/{}): {}",
                        notification.id(), attempt, properties.maxAttempts(), e.getMessage());
                if (attempt < properties.maxAttempts()) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    backoff = (long) (backoff * properties.multiplier());
                }
            }
        }
        return false;
    }
}
