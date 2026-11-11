package com.example.notifyengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param stopOnFirstMatch true: a primeira regra que resultar em SEND/DEFER encerra o processamento;
 *                         false: todas as regras que casarem são processadas
 */
@ConfigurationProperties("notify.engine")
public record EngineProperties(@DefaultValue("true") boolean stopOnFirstMatch) {
}
