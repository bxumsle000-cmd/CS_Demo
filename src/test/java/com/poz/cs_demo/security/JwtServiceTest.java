package com.poz.cs_demo.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 純單元測試，不啟動 Spring：直接 new JwtService 驗證產生 / 解析 token 的行為。
 */
class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private JwtService service(long expirationMs) {
        return new JwtService(new JwtProperties(SECRET, expirationMs));
    }

    @Test
    void 產生的token解回來是同一個agentId() {
        JwtService jwt = service(60_000);

        String token = jwt.generateToken("CSC00001");

        assertThat(token.split("\\.")).hasSize(3);          // header.payload.signature
        assertThat(jwt.parseAgentId(token)).isEqualTo("CSC00001");
    }

    @Test
    void 被竄改的token_解析失敗() {
        JwtService jwt = service(60_000);
        String token = jwt.generateToken("CSC00001");

        // 改掉簽章（第三段）的第一個字元。
        // 不能改「最後一個字元」：Base64 的最後一個字元有幾個 bit 是填充用的，
        // 改了不一定會影響解碼後的位元組，會讓這個測試時好時壞。
        int sigStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(sigStart);
        String tampered = token.substring(0, sigStart)
                + (first == 'A' ? 'B' : 'A')
                + token.substring(sigStart + 1);

        assertThatThrownBy(() -> jwt.parseAgentId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 用不同secret簽的token_解析失敗() {
        String token = new JwtService(new JwtProperties("another-secret-another-secret-another-secret", 60_000))
                .generateToken("CSC00001");

        assertThatThrownBy(() -> service(60_000).parseAgentId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 過期的token_解析失敗() throws InterruptedException {
        JwtService jwt = service(1);   // 1 毫秒後過期
        String token = jwt.generateToken("CSC00001");

        Thread.sleep(1_100);           // JWT 的 exp 精度是「秒」，所以要等超過 1 秒

        assertThatThrownBy(() -> jwt.parseAgentId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 亂七八糟的字串_解析失敗() {
        assertThatThrownBy(() -> service(60_000).parseAgentId("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}
