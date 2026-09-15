package com.poz.cs_demo.dto.auth;

/**
 * 登入成功的回應。
 *
 * @param agentId 登入者的客服代號
 * @param token   之後每次呼叫 API 要帶的登入憑證
 */
public record LoginResponse (
        String agentId,
        String token
){
}
