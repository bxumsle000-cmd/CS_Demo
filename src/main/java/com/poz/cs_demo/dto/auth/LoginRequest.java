package com.poz.cs_demo.dto.auth;

/**
 * 登入請求。
 *
 * @param agentId  客服代號，例如 CSC00001
 * @param password 明碼密碼
 */
public record LoginRequest(
        String agentId,
        String password
) {
}
