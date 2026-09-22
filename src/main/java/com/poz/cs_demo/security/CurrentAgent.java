package com.poz.cs_demo.security;

import com.poz.cs_demo.exception.ApiException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 「目前登入的客服人員」資訊提供者。
 * <p>
 * 用途：讓 Service 層可以取得「現在是誰在操作」，而不必自己去解析 Token。
 * <p>
 * 來源是 Spring Security 的 {@link SecurityContextHolder}：
 * {@link JwtAuthFilter} 在請求進來時驗證 JWT，成功就把 agentId 放進去，
 * 這裡再把它取出來。因為 SecurityContextHolder 底層是 ThreadLocal，
 * 同一個請求裡不管在哪一層呼叫都拿得到，請求結束後 Security 會自動清掉。
 */
@Component
public class CurrentAgent {

    /**
     * 取得目前登入者的 agentId。
     *
     * @throws ApiException 401：沒有登入資訊時。
     *                      正常情況下不會發生——沒登入的請求在 SecurityConfig 的授權規則就被擋掉了，
     *                      這裡是最後一道防線，避免哪天放行規則寫錯時拿到 null 往下傳。
     */
    public String currentAgentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Security 對「沒帶 token」的請求會塞一個 AnonymousAuthenticationToken（name 是 "anonymousUser"），
        // 它的 isAuthenticated() 也是 true，所以要特別排除，不能只檢查 null。
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw ApiException.unauthorized("尚未登入或登入已失效");
        }

        // JwtAuthFilter 把 agentId 放在 principal，getName() 會回傳它
        return auth.getName();
    }
}
