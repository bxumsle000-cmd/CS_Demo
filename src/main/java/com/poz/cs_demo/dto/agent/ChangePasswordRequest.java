package com.poz.cs_demo.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改自己密碼的請求。要改誰由目前登入者決定，不從 body 帶 agentId。
 *
 * @param oldPassword 舊密碼（明碼），用來確認是本人
 * @param newPassword 新密碼（明碼），Service 會轉成 BCrypt 雜湊再存；BCrypt 最多只吃 72 bytes
 */
public record ChangePasswordRequest(
        @NotBlank(message = "不可為空")
        String oldPassword,

        @NotBlank(message = "不可為空")
        @Size(min = 6, max = 72, message = "長度需介於 6 到 72 字")
        String newPassword
) {
}
