package com.example.notifyengine.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyAuthFilterTest {

    private static final String KEY = "chave-de-teste-0123456789";

    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter(KEY, new MockEnvironment());

    private MockHttpServletResponse call(ApiKeyAuthFilter f, String method, String uri, String apiKey) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (apiKey != null) {
            request.addHeader(ApiKeyAuthFilter.HEADER, apiKey);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        f.doFilter(request, response, chain);
        // 200 = a requisição seguiu pela cadeia; 401 = barrada pelo filtro
        return chain.getRequest() != null ? okResponse(response) : response;
    }

    private static MockHttpServletResponse okResponse(MockHttpServletResponse response) {
        response.setStatus(200);
        return response;
    }

    @Test
    void requestWithoutKeyIsRejected() throws Exception {
        MockHttpServletResponse response = call(filter, "POST", "/events", null);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":401");
        assertThat(response.getHeader("WWW-Authenticate")).startsWith("ApiKey");
    }

    @Test
    void requestWithWrongKeyIsRejected() throws Exception {
        assertThat(call(filter, "GET", "/rules", "chave-errada-0123456789").getStatus()).isEqualTo(401);
        assertThat(call(filter, "GET", "/rules", "").getStatus()).isEqualTo(401);
        assertThat(call(filter, "GET", "/rules", KEY + "x").getStatus()).isEqualTo(401);
        assertThat(call(filter, "GET", "/rules", KEY.substring(1)).getStatus()).isEqualTo(401);
    }

    @Test
    void requestWithCorrectKeyPasses() throws Exception {
        assertThat(call(filter, "POST", "/events", KEY).getStatus()).isEqualTo(200);
        assertThat(call(filter, "DELETE", "/rules/abc", KEY).getStatus()).isEqualTo(200);
    }

    @Test
    void healthAndStaticDocsStayPublic() throws Exception {
        assertThat(call(filter, "GET", "/actuator/health", null).getStatus()).isEqualTo(200);
        assertThat(call(filter, "GET", "/actuator/health/readiness", null).getStatus()).isEqualTo(200);
        assertThat(call(filter, "GET", "/swagger-ui/index.html", null).getStatus()).isEqualTo(200);
        assertThat(call(filter, "GET", "/v3/api-docs", null).getStatus()).isEqualTo(200);
    }

    @Test
    void metricsAndOtherActuatorEndpointsRequireTheKey() throws Exception {
        assertThat(call(filter, "GET", "/actuator/prometheus", null).getStatus()).isEqualTo(401);
        assertThat(call(filter, "GET", "/actuator/metrics", null).getStatus()).isEqualTo(401);
        assertThat(call(filter, "GET", "/decisions", null).getStatus()).isEqualTo(401);
        assertThat(call(filter, "PUT", "/users/u1/preferences", null).getStatus()).isEqualTo(401);
    }

    @Test
    void blankKeyDisablesAuthenticationOutsideProd() throws Exception {
        ApiKeyAuthFilter open = new ApiKeyAuthFilter("", new MockEnvironment());
        assertThat(call(open, "POST", "/events", null).getStatus()).isEqualTo(200);
    }

    @Test
    void blankKeyIsRefusedInProdProfile() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new ApiKeyAuthFilter("", prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NOTIFY_API_KEY");
    }

    @Test
    void tooShortKeyIsRefused() {
        assertThatThrownBy(() -> new ApiKeyAuthFilter("curta", new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("muito curta");
    }
}
