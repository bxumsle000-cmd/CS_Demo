package com.poz.cs_demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 驗證 SecurityConfig 的放行規則與 JwtAuthFilter 的整合：
 * 哪些路徑不用登入、帶不帶 token 分別會得到什麼狀態碼。
 * <p>
 * @WebMvcTest 不會載入 @Service / @ConfigurationProperties，
 * 所以 SecurityConfig 需要的 JwtService、JwtProperties 要手動帶進來（值來自 application.properties）。
 */
@WebMvcTest(controllers = SecurityConfigTest.DummyController.class)
@Import({SecurityConfig.class, JwtService.class, SecurityConfigTest.DummyController.class})
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    /** 模擬受保護的 API 與登入端點，避免依賴真正的 Controller / Service */
    @RestController
    static class DummyController {
        @GetMapping("/api/ping") String ping() { return "pong"; }
        @PostMapping("/api/auth/login") String login() { return "ok"; }
    }

    @Test
    void 受保護的API_沒帶token_401() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 受保護的API_帶有效token_200() throws Exception {
        String token = jwtService.generateToken("CSC00001");

        mockMvc.perform(get("/api/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void 受保護的API_帶無效token_401() throws Exception {
        mockMvc.perform(get("/api/ping").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 受保護的API_帶別的secret簽的token_401() throws Exception {
        String foreign = new JwtService(new JwtProperties("another-secret-another-secret-another", 60_000))
                .generateToken("CSC00001");

        mockMvc.perform(get("/api/ping").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 登入端點_沒帶token_放行() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void 靜態資源與Swagger_沒帶token_不會是401() throws Exception {
        // 切片測試沒載入 springdoc，Swagger 路徑會是 404；重點是「沒被 Security 擋成 401」
        for (String path : new String[]{"/", "/index.html", "/js/app.js", "/css/app.css",
                "/favicon.svg", "/swagger-ui/index.html", "/v3/api-docs"}) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isNotEqualTo(401);
        }
    }
}
