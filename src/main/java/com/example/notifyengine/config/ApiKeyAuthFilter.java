package com.example.notifyengine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autenticação por chave de API compartilhada, enviada no cabeçalho {@code X-API-Key}.
 * <ul>
 *   <li>Chave definida ({@code NOTIFY_API_KEY}): toda requisição precisa dela, exceto saúde e documentação estática.</li>
 *   <li>Chave vazia: autenticação desligada (só para desenvolvimento local). No perfil {@code prod} a aplicação
 *       se recusa a iniciar sem chave.</li>
 * </ul>
 * É um segredo compartilhado, sem usuários nem papéis e sem limite de tentativas: veja o SECURITY.md.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    static final int MIN_KEY_LENGTH = 16;

    /** Caminhos públicos: verificação de saúde (healthcheck do container) e documentação estática. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator/health", "/swagger-ui", "/v3/api-docs");

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private final byte[] expectedKey;

    public ApiKeyAuthFilter(@Value("${notify.security.api-key:}") String apiKey, Environment environment) {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isEmpty()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException(
                        "NOTIFY_API_KEY é obrigatório no perfil prod (mínimo " + MIN_KEY_LENGTH + " caracteres)");
            }
            log.warn("Autenticação por chave de API DESLIGADA (NOTIFY_API_KEY vazio). Use apenas em desenvolvimento local.");
            this.expectedKey = null;
            return;
        }
        if (key.length() < MIN_KEY_LENGTH) {
            throw new IllegalStateException("NOTIFY_API_KEY muito curta: use no mínimo " + MIN_KEY_LENGTH + " caracteres");
        }
        this.expectedKey = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (expectedKey == null) {
            return true;
        }
        String path = request.getRequestURI();
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        // Comparação em tempo constante: não revela quantos caracteres da chave estão corretos.
        boolean valid = provided != null
                && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expectedKey);
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "ApiKey realm=\"notify-engine\"");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Chave de API ausente ou inválida (cabeçalho X-API-Key)\"}"
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }
        chain.doFilter(request, response);
    }
}
