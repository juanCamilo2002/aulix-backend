package com.aulix.aulix_backend.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCsrfFilterTest {

    private AuthCsrfFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthCsrfFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
    }

    @Test
    void blocksMutatingCookieAuthenticatedRequestWithoutMatchingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/enrollments/courses/course-id");
        request.setCookies(
                new Cookie("accessToken", "access-token"),
                new Cookie(AuthCsrfFilter.CSRF_COOKIE, "csrf-token")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Token CSRF inválido");
    }

    @Test
    void allowsMutatingCookieAuthenticatedRequestWithMatchingHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/enrollments/courses/course-id");
        request.setCookies(
                new Cookie("accessToken", "access-token"),
                new Cookie(AuthCsrfFilter.CSRF_COOKIE, "csrf-token")
        );
        request.addHeader(AuthCsrfFilter.CSRF_HEADER, "csrf-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotRequireCsrfForBearerOnlyRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/enrollments/courses/course-id");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
