package com.poz.cs_demo.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 認證過濾器：每個請求進來時，先看有沒有帶 token，有就驗證並「標記這個請求是誰發的」。
 * <p>
 * 流程：
 * <ol>
 *   <li>讀 {@code Authorization} header，格式必須是 {@code Bearer <token>}</li>
 *   <li>沒帶、或格式不對 → 什麼都不做，直接放行（要不要擋，交給 SecurityConfig 的授權規則決定）</li>
 *   <li>有帶 → 交給 {@link JwtService} 驗證</li>
 *   <li>驗證成功 → 建一個 Authentication 放進 SecurityContext，後面的程式就能知道「現在是誰」</li>
 *   <li>驗證失敗（過期、被竄改）→ 視同沒登入，同樣放行，讓授權規則回 401</li>
 * </ol>
 * 這個 Filter 本身<b>從不</b>回 401，它只負責「辨識身分」，「要不要擋」是 SecurityConfig 的事。
 * 責任分開之後，登入頁、Swagger 這些不需要登入的路徑才能正常放行。
 * <p>
 * 注意：這個類別故意<b>不加</b> {@code @Component}。
 * Spring Boot 會把所有 Filter 型別的 Bean 自動註冊成一般的 Servlet Filter，
 * 這樣它會在 Security 的過濾鏈之外「再跑一次」，順序也不對。
 * 正確做法是在 SecurityConfig 裡 {@code new} 出來、用 {@code addFilterBefore} 掛進 Security 的鏈裡。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                String agentId = jwtService.parseAgentId(token);
                setAuthenticated(agentId, request);
            } catch (JwtException e) {
                // token 無效（過期 / 竄改 / 格式錯）：不放任何身分進 SecurityContext，
                // 後面的授權規則會因為「未登入」而回 401。這裡只記 log 方便除錯。
                log.debug("JWT 驗證失敗：{}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /** 從 Authorization header 取出 token；沒帶或不是 Bearer 格式回 null */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token;
    }

    /** 把「已通過驗證的 agentId」放進 SecurityContext，讓後續程式可以取得目前登入者 */
    private void setAuthenticated(String agentId, HttpServletRequest request) {
        // principal 放 agentId（之後 CurrentAgent 用 getName() 取回）；credentials 用不到給 null；
        // authorities 先固定給 ROLE_AGENT，目前沒有角色區分，之後要做權限時再從資料庫帶出來。
        UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken.authenticated(
                agentId, null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 官方建議：先建一個空的 context 再整個 set 進去，避免多執行緒下的競爭問題
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }
}
