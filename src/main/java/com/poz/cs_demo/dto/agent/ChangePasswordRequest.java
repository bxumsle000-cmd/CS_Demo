package com.poz.cs_demo.dto.agent;

/**
 * 修改自己密碼的請求。要改誰由目前登入者決定，不從 body 帶 agentId。
 *
 * @param oldPassword 舊密碼（明碼），用來確認是本人
 * @param newPassword 新密碼（明碼），Service 會轉成 BCrypt 雜湊再存
 */
public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
