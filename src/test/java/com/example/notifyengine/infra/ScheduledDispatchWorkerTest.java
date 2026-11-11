package com.example.notifyengine.infra;

import com.example.notifyengine.application.AuditService;
import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.DecisionType;
import com.example.notifyengine.domain.model.Notification;
import com.example.notifyengine.domain.port.OptOutPort;
import com.example.notifyengine.domain.port.ScheduledQueue;
import com.example.notifyengine.infra.channel.NotificationDispatcher;
import com.example.notifyengine.infra.scheduler.ScheduledDispatchWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regressão: opt-out feito depois do agendamento deve ser respeitado no momento do envio. */
class ScheduledDispatchWorkerTest {

    private final Notification notification = new Notification(UUID.randomUUID(), "e1", "PEDIDO_CRIADO", "u1",
            UUID.randomUUID(), Channel.EMAIL, "ana@example.com", "oi");

    private ScheduledQueue queue;
    private NotificationDispatcher dispatcher;
    private OptOutPort optOut;
    private AuditService audit;
    private ScheduledDispatchWorker worker;

    @BeforeEach
    void setUp() {
        queue = mock(ScheduledQueue.class);
        dispatcher = mock(NotificationDispatcher.class);
        optOut = mock(OptOutPort.class);
        audit = mock(AuditService.class);
        worker = new ScheduledDispatchWorker(queue, dispatcher, optOut, audit, Clock.systemUTC(), 10);
        when(queue.claimDue(any(), anyInt())).thenReturn(List.of(notification));
    }

    @Test
    void doesNotSendWhenUserOptedOutAfterScheduling() {
        when(optOut.isOptedOut("u1", Channel.EMAIL, "PEDIDO_CRIADO")).thenReturn(true);

        worker.dispatchDue();

        verify(dispatcher, never()).dispatch(any());
        verify(audit).recordDeferredDispatch(notification, DecisionType.BLOCK, "USER_OPTED_OUT");
    }

    @Test
    void sendsWhenUserStillAcceptsNotifications() {
        when(optOut.isOptedOut("u1", Channel.EMAIL, "PEDIDO_CRIADO")).thenReturn(false);
        when(dispatcher.dispatch(notification)).thenReturn(true);

        worker.dispatchDue();

        verify(audit).recordDeferredDispatch(notification, DecisionType.SEND, "DEFERRED_NOTIFICATION_SENT");
    }
}
