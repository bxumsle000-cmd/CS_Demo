package com.poz.cs_demo.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 純單元測試，不啟動 Spring：用 Mock 的 request / response / chain 直接呼叫 Filter，
 * 驗證「有沒有把正確的身分放進 SecurityContext」以及「無論如何都會放行」。
 */
class JwtAuthFilterTest {

    private final JwtService jwtService =
            new JwtService(new JwtProperties("0123456789abcdef0123456789abcdef", 60_000));
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService);

    @AfterEach
    void clearContext() {
        // SecurityContextHolder 預設是 ThreadLocal，測試之間要清掉，避免互相污染
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain doFilter(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tickets");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void 有效token_SecurityContext裡是該agentId且有放行() throws Exception {
        String token = jwtService.generateToken("CSC00001");

        MockFilterChain chain = doFilter("Bearer " + token);

        assertThat(currentAuth()).isNotNull();
        assertThat(currentAuth().getName()).isEqualTo("CSC00001");
        assertThat(currentAuth().isAuthenticated()).isTrue();
        assertThat(currentAuth().getAuthorities()).extracting("authority").containsExactly("ROLE_AGENT");
        assertThat(chain.getRequest()).as("有呼叫 chain.doFilter").isNotNull();
    }

    @Test
    void 沒帶header_不設定身分但仍放行() throws Exception {
        MockFilterChain chain = doFilter(null);

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 不是Bearer格式_不設定身分但仍放行() throws Exception {
        MockFilterChain chain = doFilter("Basic abc123");

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 無效token_不設定身分但仍放行_不丟例外() throws Exception {
        MockFilterChain chain = doFilter("Bearer this.is.garbage");

        assertThat(currentAuth()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 用別的secret簽的token_視同無效() throws Exception {
        String foreign = new JwtService(new JwtProperties("another-secret-another-secret-another", 60_000))
                .generateToken("CSC00001");

        doFilter("Bearer " + foreign);

        assertThat(currentAuth()).isNull();
    }
}
