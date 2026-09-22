package com.poz.cs_demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 的產生與解析。
 * <p>
 * 這個類別只負責「token ↔ agentId」的轉換，不碰資料庫、也不碰 Spring Security，
 * 所以可以單獨用單元測試驗證。
 * <p>
 * 一顆 JWT 長這樣：{@code header.payload.signature}（三段 Base64Url 字串用「.」隔開）
 * <ul>
 *   <li>header：演算法（這裡是 HS256）</li>
 *   <li>payload：我們放進去的資料（sub = agentId、iat = 簽發時間、exp = 過期時間）</li>
 *   <li>signature：用 secret 對前兩段算出的簽章，用來證明「這是我發的、沒被改過」</li>
 * </ul>
 * payload 只是 Base64 編碼、<b>不是加密</b>，任何人都能解開來看，所以不要放密碼等敏感資料。
 */
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(JwtProperties props) {
        // 把設定檔的字串轉成 HMAC 用的 SecretKey；長度不足 256 bit 會在這裡丟 WeakKeyException
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = props.expirationMs();
    }

    /**
     * 登入成功後產生 token。
     *
     * @param agentId 登入者的客服代號，會放在 JWT 的 sub（subject）欄位
     * @return 簽好名的 token 字串
     */
    public String generateToken(String agentId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(agentId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 驗證 token 並取出 agentId。
     * <p>
     * 驗證內容：簽章是否正確（沒被竄改、是我們發的）、是否過期。
     *
     * @param token 前端帶來的 token（不含 "Bearer " 前綴）
     * @return token 裡的 agentId
     * @throws JwtException 簽章錯誤、格式錯誤、已過期等任何驗證失敗的情況
     *                      （過期是其子類別 {@link io.jsonwebtoken.ExpiredJwtException}）
     */
    public String parseAgentId(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
}
