package com.example.notifyengine.infra.redis;

import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.port.ScheduledQueue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fila de notificações adiadas em um sorted set do Redis (score = epoch millis do envio).
 * {@link #claimDue} usa ZREM como "claim": só quem remover o membro despacha, o que torna
 * seguro rodar várias instâncias do worker.
 */
@Component
@ConditionalOnProperty(name = "notify.store", havingValue = "redis", matchIfMissing = true)
public class RedisNotificationScheduler implements ScheduledQueue {

    public static final String QUEUE_KEY = "schedule:queue";
    private static final Logger log = LoggerFactory.getLogger(RedisNotificationScheduler.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisNotificationScheduler(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public void schedule(Notification notification, Instant when) {
        try {
            redis.opsForZSet().add(QUEUE_KEY, mapper.writeValueAsString(notification), when.toEpochMilli());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar notificação agendada", e);
        }
    }

    @Override
    public List<Notification> claimDue(Instant now, int limit) {
        Set<String> due = redis.opsForZSet().rangeByScore(QUEUE_KEY, 0, now.toEpochMilli(), 0, limit);
        List<Notification> claimed = new ArrayList<>();
        if (due == null) {
            return claimed;
        }
        for (String member : due) {
            Long removed = redis.opsForZSet().remove(QUEUE_KEY, member);
            if (removed != null && removed > 0) {
                try {
                    claimed.add(mapper.readValue(member, Notification.class));
                } catch (JsonProcessingException e) {
                    log.error("Notificação agendada inválida descartada: {}", member, e);
                }
            }
        }
        return claimed;
    }
}
