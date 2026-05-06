package com.aulix.aulix_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthRateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "maxRequests", 2);
        ReflectionTestUtils.setField(filter, "windowSeconds", 60L);
        ReflectionTestUtils.setField(filter, "trustForwardedHeaders", false);
    }

    @Test
    void blocksAuthEndpointAfterConfiguredLimit() throws Exception {
        MockHttpServletResponse firstResponse = executeLoginRequest();
        MockHttpServletResponse secondResponse = executeLoginRequest();
        MockHttpServletResponse thirdResponse = executeLoginRequest();

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getHeader(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    @Test
    void doesNotLimitNonAuthEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/courses");
        request.setServletPath("/courses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresForwardedForHeaderByDefault() throws Exception {
        executeLoginRequest("192.0.2.10", "198.51.100.1");
        executeLoginRequest("192.0.2.10", "198.51.100.2");
        MockHttpServletResponse thirdResponse = executeLoginRequest("192.0.2.10", "198.51.100.3");

        assertThat(thirdResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void usesForwardedForHeaderOnlyWhenTrusted() throws Exception {
        ReflectionTestUtils.setField(filter, "trustForwardedHeaders", true);

        executeLoginRequest("192.0.2.10", "198.51.100.1");
        executeLoginRequest("192.0.2.10", "198.51.100.2");
        MockHttpServletResponse thirdResponse = executeLoginRequest("192.0.2.10", "198.51.100.3");

        assertThat(thirdResponse.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse executeLoginRequest() throws Exception {
        return executeLoginRequest("192.0.2.10", null);
    }

    private MockHttpServletResponse executeLoginRequest(String remoteAddr, String forwardedFor) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        return response;
    }
}
