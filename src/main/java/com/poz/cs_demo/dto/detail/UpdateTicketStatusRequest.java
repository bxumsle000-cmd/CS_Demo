package com.poz.cs_demo.dto.detail;

import com.poz.cs_demo.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 工單詳情頁「變更工單狀態」的請求。
 * 名稱加上 Ticket 前綴，避免與客服端的 UpdateAgentStatusRequest 混淆。
 * 操作者由 CurrentAgent 取得，不從 body 帶。
 *
 * @param status 新狀態：待客戶回覆 = PENDING、已解決 = RESOLVED、處理中 = IN_PROGRESS
 */
public record UpdateTicketStatusRequest(
        @NotNull(message = "不可為空")
        TicketStatus status
) {
}
