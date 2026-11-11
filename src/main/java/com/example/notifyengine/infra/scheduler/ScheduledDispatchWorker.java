package com.example.notifyengine.infra.scheduler;

import com.example.notifyengine.application.AuditService;
import com.example.notifyengine.domain.model.DecisionType;
import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.port.OptOutPort;
import com.example.notifyengine.domain.port.ScheduledQueue;
import com.example.notifyengine.infra.channel.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/** Worker que despacha, no horário certo, as notificações adiadas guardadas na fila. */
@Component
public class ScheduledDispatchWorker {

    private static final Logger log = LoggerFactory.getLogger(ScheduledDispatchWorker.class);

    private final ScheduledQueue queue;
    private final NotificationDispatcher dispatcher;
    private final OptOutPort optOut;
    private final AuditService audit;
    private final Clock clock;
    private final int batchSize;

    public ScheduledDispatchWorker(ScheduledQueue queue,
                                   NotificationDispatcher dispatcher,
                                   OptOutPort optOut,
                                   AuditService audit,
                                   Clock clock,
                                   @Value("${notify.scheduler.batch-size:50}") int batchSize) {
        this.queue = queue;
        this.dispatcher = dispatcher;
        this.optOut = optOut;
        this.audit = audit;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notify.scheduler.poll-interval-ms:5000}")
    public void dispatchDue() {
        try {
            List<Notification> due = queue.claimDue(clock.instant(), batchSize);
            for (Notification notification : due) {
                process(notification);
            }
            if (!due.isEmpty()) {
                log.info("{} notificação(ões) agendada(s) processada(s)", due.size());
            }
        } catch (Exception e) {
            log.error("Erro no worker de notificações agendadas", e);
        }
    }

    private void process(Notification notification) {
        // O usuário pode ter feito opt-out entre o agendamento e o horário de envio.
        if (optOut.isOptedOut(notification.userId(), notification.channel(), notification.eventType())) {
            audit.recordDeferredDispatch(notification, DecisionType.BLOCK, "USER_OPTED_OUT");
            return;
        }
        boolean sent = dispatcher.dispatch(notification);
        audit.recordDeferredDispatch(notification,
                sent ? DecisionType.SEND : DecisionType.BLOCK,
                sent ? "DEFERRED_NOTIFICATION_SENT" : "DEFERRED_DISPATCH_FAILED");
    }
}
