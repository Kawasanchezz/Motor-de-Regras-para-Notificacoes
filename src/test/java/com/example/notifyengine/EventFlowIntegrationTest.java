package com.example.notifyengine;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste ponta a ponta com Postgres e Redis reais (Testcontainers). Requer Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventFlowIntegrationTest {

    // O ciclo de vida dos containers é gerenciado pelo @Container (Testcontainers).
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = redisContainer();

    private static GenericContainer<?> redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"));
        container.addExposedPort(6379);
        return container;
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    StringRedisTemplate redisTemplate;

    private static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String ruleJson(String eventType, String constraints) {
        return """
                {"name":"IT rule","description":"it","eventType":"%s","priority":1,"active":true,
                 "condition":{"type":"AND","children":[{"type":"LEAF","field":"order.total","op":"GT","value":10}]},
                 "actions":[{"channel":"EMAIL","template":"Oi {{user.name}}, pedido {{order.id}}","recipient":"user.email"}],
                 "constraints":%s}
                """.formatted(eventType, constraints);
    }

    private static String eventJson(String eventId, String eventType, String userId, int total) {
        return """
                {"eventId":"%s","eventType":"%s","userId":"%s",
                 "payload":{"order":{"id":"A1","total":%d}},
                 "userAttributes":{"name":"Ana","email":"ana@example.com"}}
                """.formatted(eventId, eventType, userId, total);
    }

    private String createRule(String eventType, String constraints) throws Exception {
        String body = mvc.perform(post("/rules").contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson(eventType, constraints)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions sendEvent(String eventId, String eventType, String userId, int total) throws Exception {
        return mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
                .content(eventJson(eventId, eventType, userId, total)));
    }

    @Test
    void matchingEventIsSentAndAudited() throws Exception {
        String eventType = unique("IT_SEND");
        String userId = unique("user");
        createRule(eventType, "[]");

        sendEvent(unique("evt"), eventType, userId, 50)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("SEND"))
                .andExpect(jsonPath("$.deliveries[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$.replayed").value(false));

        mvc.perform(get("/decisions").param("userId", userId).param("decision", "SEND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventType").value(eventType));
    }

    @Test
    void nonMatchingEventIsBlocked() throws Exception {
        String eventType = unique("IT_NOMATCH");
        createRule(eventType, "[]");

        sendEvent(unique("evt"), eventType, unique("user"), 5)
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.reason").value("NO_MATCHING_RULE"));
    }

    @Test
    void eventWithoutRulesIsBlocked() throws Exception {
        sendEvent(unique("evt"), unique("IT_NORULES"), unique("user"), 50)
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.reason").value("NO_ACTIVE_RULES"));
    }

    @Test
    void sameEventIdIsIdempotent() throws Exception {
        String eventType = unique("IT_IDEM");
        String eventId = unique("evt");
        String userId = unique("user");
        createRule(eventType, "[]");

        sendEvent(eventId, eventType, userId, 50).andExpect(jsonPath("$.replayed").value(false));
        sendEvent(eventId, eventType, userId, 50)
                .andExpect(jsonPath("$.decision").value("SEND"))
                .andExpect(jsonPath("$.replayed").value(true));

        mvc.perform(get("/decisions").param("userId", userId))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void cooldownBlocksSecondEventForSameUser() throws Exception {
        String eventType = unique("IT_COOLDOWN");
        String userId = unique("user");
        createRule(eventType, "[{\"type\":\"COOLDOWN\",\"params\":{\"seconds\":3600}}]");

        sendEvent(unique("evt"), eventType, userId, 50).andExpect(jsonPath("$.decision").value("SEND"));
        sendEvent(unique("evt"), eventType, userId, 50)
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.reason").value("COOLDOWN_ACTIVE"));
        sendEvent(unique("evt"), eventType, unique("other"), 50)
                .andExpect(jsonPath("$.decision").value("SEND"));
    }

    @Test
    void rateLimitBlocksAfterMax() throws Exception {
        String eventType = unique("IT_RATE");
        String userId = unique("user");
        createRule(eventType, "[{\"type\":\"RATE_LIMIT\",\"params\":{\"max\":2,\"windowSeconds\":3600}}]");

        sendEvent(unique("evt"), eventType, userId, 50).andExpect(jsonPath("$.decision").value("SEND"));
        sendEvent(unique("evt"), eventType, userId, 50).andExpect(jsonPath("$.decision").value("SEND"));
        sendEvent(unique("evt"), eventType, userId, 50)
                .andExpect(jsonPath("$.reason").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void optOutBlocksSend() throws Exception {
        String eventType = unique("IT_OPTOUT");
        String userId = unique("user");
        createRule(eventType, "[]");

        mvc.perform(put("/users/{id}/preferences", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferences\":[{\"channel\":\"EMAIL\",\"optedOut\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].optedOut").value(true));

        sendEvent(unique("evt"), eventType, userId, 50)
                .andExpect(jsonPath("$.decision").value("BLOCK"))
                .andExpect(jsonPath("$.reason").value("USER_OPTED_OUT"));

        mvc.perform(put("/users/{id}/preferences", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferences\":[{\"channel\":\"EMAIL\",\"optedOut\":false}]}"))
                .andExpect(status().isOk());

        sendEvent(unique("evt"), eventType, userId, 50).andExpect(jsonPath("$.decision").value("SEND"));
    }

    @Test
    void eventOutsideTimeWindowIsDeferredToRedisQueue() throws Exception {
        String eventType = unique("IT_DEFER");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        String constraint = "[{\"type\":\"TIME_WINDOW\",\"params\":{\"start\":\"%s\",\"end\":\"%s\",\"zone\":\"UTC\"}}]"
                .formatted(now.plusHours(2).format(fmt), now.plusHours(3).format(fmt));
        createRule(eventType, constraint);

        Long before = redisTemplate.opsForZSet().zCard("schedule:queue");
        sendEvent(unique("evt"), eventType, unique("user"), 50)
                .andExpect(jsonPath("$.decision").value("DEFER"))
                .andExpect(jsonPath("$.reason").value("OUTSIDE_TIME_WINDOW"))
                .andExpect(jsonPath("$.deliveries[0].scheduledFor").exists());

        Long after = redisTemplate.opsForZSet().zCard("schedule:queue");
        assertThat(after).isEqualTo((before == null ? 0 : before) + 1);
    }

    @Test
    void deactivatingRuleInvalidatesCache() throws Exception {
        String eventType = unique("IT_CACHE");
        String ruleId = createRule(eventType, "[]");

        sendEvent(unique("evt"), eventType, unique("user"), 50).andExpect(jsonPath("$.decision").value("SEND"));

        mvc.perform(patch("/rules/{id}/status", ruleId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        sendEvent(unique("evt"), eventType, unique("user"), 50)
                .andExpect(jsonPath("$.reason").value("NO_ACTIVE_RULES"));
    }

    @Test
    void ruleCrudLifecycle() throws Exception {
        String eventType = unique("IT_CRUD");
        String ruleId = createRule(eventType, "[]");

        mvc.perform(get("/rules/{id}", ruleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value(eventType));

        mvc.perform(put("/rules/{id}", ruleId).contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson(eventType, "[{\"type\":\"COOLDOWN\",\"params\":{\"seconds\":10}}]")
                                .replace("IT rule", "Renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.constraints[0].type").value("COOLDOWN"));

        mvc.perform(get("/rules").param("eventType", eventType))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mvc.perform(delete("/rules/{id}", ruleId)).andExpect(status().isNoContent());
        mvc.perform(get("/rules/{id}", ruleId)).andExpect(status().isNotFound());
    }

    @Test
    void invalidRulesAreRejectedWithStandardError() throws Exception {
        mvc.perform(post("/rules").contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson("X", "[]").replace("\"GT\"", "\"BOGUS\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        mvc.perform(post("/rules").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());

        mvc.perform(post("/rules").contentType(MediaType.APPLICATION_JSON)
                        .content(ruleJson("X", "[{\"type\":\"COOLDOWN\",\"params\":{}}]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedFieldsAreRejected() throws Exception {
        String tooLong = "x".repeat(200);
        sendEvent(tooLong, "E", "u", 50)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());

        mvc.perform(get("/users/{id}/preferences", tooLong)).andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonDoesNotLeakInternalClassNames() throws Exception {
        mvc.perform(post("/rules").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actions\":[{\"channel\":\"FAX\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("com.example"))))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void requestBodyLargerThanLimitIsRejected() throws Exception {
        String huge = "{\"eventId\":\"big\",\"eventType\":\"E\",\"userId\":\"u\",\"payload\":{\"x\":\""
                + "a".repeat(300_000) + "\"}}";
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(huge))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void oversizedIdempotencyKeyIsRejectedBeforeAnyNotificationIsSent() throws Exception {
        String eventType = unique("IT_IDEMKEY");
        String userId = unique("user");
        createRule(eventType, "[]");

        mvc.perform(post("/events").header("Idempotency-Key", "k".repeat(200))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(unique("evt"), eventType, userId, 50)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/decisions").param("userId", userId))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        mvc.perform(get("/rules").param("size", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void seededSampleRulesAreAvailable() throws Exception {
        mvc.perform(get("/rules").param("eventType", "PAGAMENTO_FALHOU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actions.length()").value(2));
    }
}
