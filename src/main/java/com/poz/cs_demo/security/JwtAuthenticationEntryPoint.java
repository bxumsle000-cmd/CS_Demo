package com.poz.cs_demo.security;

import com.poz.cs_demo.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未登入（或 token 無效）卻打到需要登入的路徑時，負責產生 401 回應。
 * <p>
 * 為什麼不能交給 GlobalExceptionHandler：
 * Security 的攔截發生在 Filter 層，在 DispatcherServlet 之前，
 * 請求根本沒進到 Controller，@RestControllerAdvice 完全沒機會出手。
 * 所以要在這裡自己把 {@link ErrorResponse} 寫成 JSON，
 * 讓前端不論錯誤來自 Security 還是 Controller，拿到的格式都一樣。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse body = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "尚未登入或登入已失效");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
