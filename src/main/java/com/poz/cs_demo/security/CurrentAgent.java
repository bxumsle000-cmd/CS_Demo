package com.poz.cs_demo.security;

import org.springframework.stereotype.Component;

/**
 * 「目前登入的客服人員」資訊提供者。
 * <p>
 * 用途：讓 Service 層（例如 AuthService）可以取得「現在是誰在操作」，
 * 而不必自己去解析 Token 或 Session。
 * <p>
 * 目前為【開發用暫時版本】：
 * 尚未接上真正的登入驗證（JWT / Session），所以先固定回傳寫死的 agentId 與 token，
 * 方便在還沒做完認證機制前，先開發、測試其他功能。
 * <p>
 * 之後接上 Spring Security 時，應改為從 SecurityContext（或 JWT）取出真正的使用者資訊。
 */
@Component
public class CurrentAgent {
    private static final String DEV_CURRENT_AGENT_ID = "CSC00001";
    private static final String DEV_CURRENT_TOKEN = "DEV_CURRENT_TOKEN";

    public String currentAgentId() {
        return DEV_CURRENT_AGENT_ID;
    }

    public String currentToken() {
        return DEV_CURRENT_TOKEN;
    }

}
