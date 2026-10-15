package com.poz.cs_demo.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 建立客服帳號的請求。
 *
 * @param agentId  客服代號，例如 CSC00001，不可與現有帳號重複；對應 agents.agent_id NVARCHAR(10)
 * @param name     客服姓名，對應 agents.name NVARCHAR(50)
 * @param password 明碼密碼，Service 會轉成 BCrypt 雜湊再存；BCrypt 最多只吃 72 bytes
 */
public record RegisterRequest(
        @NotBlank(message = "不可為空")
        @Size(max = 10, message = "長度不可超過 10 字")
        String agentId,

        @NotBlank(message = "不可為空")
        @Size(max = 50, message = "長度不可超過 50 字")
        String name,

        @NotBlank(message = "不可為空")
        @Size(min = 6, max = 72, message = "長度需介於 6 到 72 字")
        String password
) {

}
