package com.poz.cs_demo.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登入請求。
 *
 * @param agentId  客服代號，例如 CSC00001
 * @param password 明碼密碼
 */
public record LoginRequest(
        @NotBlank(message = "不可為空")
        String agentId,

        @NotBlank(message = "不可為空")
        String password
) {
}
