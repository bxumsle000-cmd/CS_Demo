package com.poz.cs_demo.exception;

import com.poz.cs_demo.security.JwtAuthenticationEntryPoint;
import com.poz.cs_demo.security.JwtProperties;
import com.poz.cs_demo.security.JwtService;
import com.poz.cs_demo.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 只載入 web 層（不連資料庫），用一個測試專用 Controller 觸發各種例外，
 * 驗證 GlobalExceptionHandler 回傳的狀態碼與 JSON 內容。
 * <p>
 * 關於 Security 的三個註解：
 * <ul>
 *   <li>@Import SecurityConfig：@WebMvcTest 會載入 Spring Security，但不會掃到專案自己的 SecurityConfig，
 *       會退回「全部端點鎖住」的預設</li>
 *   <li>@Import JwtService、JwtAuthenticationEntryPoint + @EnableConfigurationProperties：
 *       SecurityConfig 需要它們，切片測試不會自動載入</li>
 *   <li>@WithMockUser：/test/** 不在放行名單內，要模擬「已登入」請求才進得到 Controller，
 *       否則會在 Security 層就被擋成 401，根本測不到 GlobalExceptionHandler</li>
 * </ul>
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class,
        GlobalExceptionHandlerTest.DummyController.class})
@EnableConfigurationProperties(JwtProperties.class)
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    record DummyRequest(@NotBlank(message = "不可為空") String name) {}

    @RestController
    static class DummyController {
        @GetMapping("/test/not-found")
        void notFound() { throw ApiException.notFound("找不到工單：T001"); }

        @GetMapping("/test/boom")
        void boom() { throw new IllegalStateException("內部細節不該外洩"); }

        @PostMapping("/test/valid")
        void valid(@Valid @RequestBody DummyRequest req) {}
    }

    @Test
    void apiException_回傳對應狀態碼與訊息() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("找不到工單：T001"));
    }

    @Test
    void 驗證失敗_回傳400並列出欄位錯誤() throws Exception {
        mockMvc.perform(post("/test/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("name: 不可為空"));
    }

    @Test
    void 未預期例外_回傳500且不洩漏細節() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系統發生錯誤，請稍後再試"));
    }
}
