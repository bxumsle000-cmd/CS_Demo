package com.poz.cs_demo.dto.agent;

import com.poz.cs_demo.enums.AgentStatus;

/**
 * 客服變更自己工作狀態的請求。
 * 名稱加上 Agent 前綴，避免與工單端的 UpdateTicketStatusRequest 混淆。
 *
 * @param status 新的工作狀態；ON_CALL 由系統設定，不可手動選擇
 */
public record UpdateAgentStatusRequest(
    AgentStatus status
) {
}
