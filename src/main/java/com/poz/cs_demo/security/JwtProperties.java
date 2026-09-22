package com.poz.cs_demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 相關設定值，對應 application.properties 裡 {@code jwt.*} 的項目。
 * <p>
 * Spring Boot 會在啟動時把設定檔的值綁進這個 record（record 的欄位名稱對應設定 key，
 * {@code expiration-ms} 會自動對應到 {@code expirationMs}）。
 * 其他類別要用時直接注入 {@code JwtProperties} 即可，不必到處寫 @Value。
 * <p>
 * 之所以要在 {@link com.poz.cs_demo.CsDemoApplication} 加 {@code @ConfigurationPropertiesScan}，
 * 是因為 @ConfigurationProperties 本身不會註冊 Bean，需要有人「掃描」它。
 *
 * @param secret       簽章金鑰，HS256 要求至少 32 bytes
 * @param expirationMs token 有效時間（毫秒）
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
    /** HS256 的最低金鑰長度：256 bit = 32 bytes */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * record 的「精簡建構子」：在物件建立前先檢查設定值是否合理，
     * 讓設定錯誤在「啟動當下」就爆出來，而不是等到第一次登入才發現。
     */
    public JwtProperties {
        if (secret == null || secret.getBytes().length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "jwt.secret 至少要 " + MIN_SECRET_BYTES + " bytes，請檢查 application.properties 或環境變數 JWT_SECRET");
        }
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("jwt.expiration-ms 必須大於 0");
        }
    }
}
