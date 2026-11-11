package com.example.notifyengine.infra.memory;

import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.port.ScheduledQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Fila agendada em memória (apenas desenvolvimento local; perde os itens ao reiniciar). */
@Component
@ConditionalOnProperty(name = "notify.store", havingValue = "memory")
public class InMemoryScheduledQueue implements ScheduledQueue {

    private record Item(Notification notification, long dueMillis) {
    }

    private final List<Item> items = new ArrayList<>();

    @Override
    public synchronized void schedule(Notification notification, Instant when) {
        items.add(new Item(notification, when.toEpochMilli()));
    }

    @Override
    public synchronized List<Notification> claimDue(Instant now, int limit) {
        long nowMillis = now.toEpochMilli();
        items.sort(Comparator.comparingLong(Item::dueMillis));
        List<Notification> claimed = new ArrayList<>();
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext() && claimed.size() < limit) {
            Item item = iterator.next();
            if (item.dueMillis() > nowMillis) {
                break;
            }
            claimed.add(item.notification());
            iterator.remove();
        }
        return claimed;
    }
}
