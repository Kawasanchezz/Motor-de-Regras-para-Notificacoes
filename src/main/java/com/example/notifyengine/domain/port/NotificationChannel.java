package com.example.notifyengine.domain.port;

import com.example.notifyengine.domain.model.Channel;
import com.example.notifyengine.domain.model.Notification;

/**
 * Ponto de extensão: para adicionar um canal novo basta criar um bean que implemente esta interface.
 */
public interface NotificationChannel {

    Channel type();

    /** Deve lançar exceção em caso de falha para que o despachante faça retry. */
    void send(Notification notification) throws Exception;
}
