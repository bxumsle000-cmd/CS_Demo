package com.poz.cs_demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密碼加密設定。
 * <p>
 * 負責向 Spring 容器註冊一個 {@link PasswordEncoder} Bean，
 * 讓其他地方（例如 AuthService）可以直接用 @Autowired / 建構子注入取得，
 * 用來：
 * <ul>
 *   <li>註冊、改密碼時：{@code passwordEncoder.encode(明文)} → 產生雜湊值存進資料庫</li>
 *   <li>登入時：{@code passwordEncoder.matches(明文, 資料庫雜湊值)} → 比對密碼是否正確</li>
 * </ul>
 * 資料庫永遠只存雜湊值，不存明文密碼。
 */
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
