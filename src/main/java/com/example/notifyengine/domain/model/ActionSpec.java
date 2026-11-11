package com.example.notifyengine.domain.model;

/**
 * Ação de uma regra: canal, template da mensagem e expressão do destinatário
 * (caminho no contexto, ex.: {@code user.email}, ou valor literal).
 */
public record ActionSpec(Channel channel, String template, String recipient) {
}
