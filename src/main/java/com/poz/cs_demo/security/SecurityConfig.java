package com.poz.cs_demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 主要設定。
 * <p>
 * 只要 spring-boot-starter-security 在 classpath，Spring Security 預設會：
 * <ul>
 *   <li>把所有端點鎖住（未登入回 401）</li>
 *   <li>啟用 Session、CSRF、表單登入、HTTP Basic</li>
 * </ul>
 * 這些預設是為「傳統伺服器渲染網頁」設計的，不適合前後端分離 + JWT 的架構，
 * 所以在這裡全部改掉，改成：
 * <ul>
 *   <li>每個請求先經過 {@link JwtAuthFilter}，有帶有效 token 就標記身分</li>
 *   <li>登入、靜態資源、Swagger 不需要登入；其餘 /api/** 一律要登入</li>
 *   <li>未登入打到受保護的路徑 → 401</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    /**
     * 不需要登入就能存取的路徑。
     * 前端是單頁應用（hash 路由），所以靜態資源只有這幾個；Swagger 用 springdoc 的預設路徑。
     * /error 是 Spring Boot 內建的錯誤頁路徑，不放行的話錯誤會被蓋成 401，看不到真正的原因。
     */
    private static final String[] PUBLIC_PATHS = {
            "/", "/index.html", "/css/**", "/js/**", "/favicon.svg",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 是防「瀏覽器自動帶 Cookie」的攻擊；JWT 放在 Authorization header，
                // 瀏覽器不會自動帶，所以不需要 CSRF 保護，關掉以免 POST / PUT 被擋。
                // STATELESS：伺服器不建立也不讀取 HttpSession，
                // 每個請求都只靠自己帶的 JWT 證明身分。
                // 前後端分離不需要 Security 內建的登入頁與 Basic 認證視窗，
                // 登入走自己的 /api/auth/login。
                // 未登入（SecurityContext 裡沒有身分）卻打到需要登入的路徑時怎麼回應。
                // 關掉 formLogin / httpBasic 之後 Security 預設會回 403，這裡明確改成 401。

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 401 的回應內容交給 JwtAuthenticationEntryPoint，回跟 GlobalExceptionHandler 同格式的 JSON。
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))

                // 授權規則：由上往下逐條比對，第一條符合的生效，所以「放行」要寫在「要登入」前面。
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )

                // 把 JwtAuthFilter 掛進 Security 的過濾鏈，放在「表單登入 Filter」的位置之前，
                // 讓授權規則判斷時 SecurityContext 已經有身分了。
                // 這裡直接 new，而不是注入 Bean：JwtAuthFilter 故意沒加 @Component，原因見該類別的說明。
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
