package com.poz.cs_demo.dto.agent;

/**
 * 建立客服帳號的請求。
 *
 * @param agentId  客服代號，例如 CSC00001，不可與現有帳號重複
 * @param name     客服姓名
 * @param password 明碼密碼，Service 會轉成 BCrypt 雜湊再存
 */
public record RegisterRequest(
        String agentId,
        String name,
        String password
) {

}
