package com.poz.cs_demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 主要設定。
 * <p>
 * 只要 spring-boot-starter-security 在 classpath，Spring Security 預設會：
 * <ul>
 *   <li>把所有端點鎖住（未登入回 401）</li>
 *   <li>啟用 Session、CSRF、表單登入、HTTP Basic</li>
 * </ul>
 * 這些預設是為「傳統伺服器渲染網頁」設計的，不適合前後端分離 + JWT 的架構，
 * 所以在這裡全部改掉。
 * <p>
 * 目前為【導入 JWT 的第一階段】：
 * 先把 Security 接進來、關掉不需要的機制，但所有請求暫時一律放行（permitAll），
 * 讓專案維持原本的行為。等 JwtAuthFilter 完成後，再把放行規則收緊。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 是防「瀏覽器自動帶 Cookie」的攻擊；JWT 放在 Authorization header，
                // 瀏覽器不會自動帶，所以不需要 CSRF 保護，關掉以免 POST / PUT 被擋。
                // STATELESS：伺服器不建立也不讀取 HttpSession，
                // 每個請求都只靠自己帶的 JWT 證明身分。
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 第一階段：全部放行，行為與加 Security 之前完全相同。
                // TODO 完成 JwtAuthFilter 後改為：
                //   /api/auth/login、靜態資源、Swagger → permitAll
                //   其餘 /api/** → authenticated
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
