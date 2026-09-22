package com.poz.cs_demo.security;

import com.poz.cs_demo.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 純單元測試：直接操作 SecurityContextHolder，驗證 CurrentAgent 的取值與防呆。
 */
class CurrentAgentTest {

    private final CurrentAgent currentAgent = new CurrentAgent();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(Authentication auth) {
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 已登入_回傳SecurityContext裡的agentId() {
        setAuth(UsernamePasswordAuthenticationToken.authenticated(
                "CSC00001", null, List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));

        assertThat(currentAgent.currentAgentId()).isEqualTo("CSC00001");
    }

    @Test
    void 沒有任何身分_401() {
        assertThatThrownBy(currentAgent::currentAgentId)
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 匿名身分_401() {
        setAuth(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(currentAgent::currentAgentId)
                .isInstanceOf(ApiException.class);
    }
}
