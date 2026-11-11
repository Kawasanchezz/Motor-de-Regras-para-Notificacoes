package com.example.notifyengine.domain.port;

import com.example.notifyengine.domain.model.Notification;

import java.time.Instant;
import java.util.List;

/** Fila de notificações adiadas: agenda e entrega (uma única vez) as que já venceram. */
public interface ScheduledQueue extends NotificationScheduler {

    /** Remove e devolve até {@code limit} notificações cujo horário já passou. */
    List<Notification> claimDue(Instant now, int limit);
}
