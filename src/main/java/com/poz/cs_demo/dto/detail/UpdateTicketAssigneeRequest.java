package com.poz.cs_demo.dto.detail;

/**
 * 工單詳情頁「轉接工單」的請求。
 * 只帶要轉給誰，操作者（轉出的人）由 CurrentAgent 取得，不從 body 帶。
 *
 * @param assignId 要轉接給的客服代號，例如 CSC00002（對應 agents.agent_id）
 */
public record UpdateTicketAssigneeRequest(
        String assignId
) {
}
