package com.poz.cs_demo.dto.ticket;

import com.poz.cs_demo.entity.Agent;

/**
 * 「轉派給其他客服」下拉選單的一個選項，畫面上顯示成「CSC00002 陳美芳」。
 * 只給代號與姓名，刻意不含密碼雜湊與工作狀態。
 *
 * @param agentId 客服代號，例如 CSC00002；送出轉派時要帶回後端
 * @param name    客服姓名
 */
public record AssigneeResponse(
        String agentId,
        String name
) {
    /** 從 Entity 轉成 Response，只挑下拉選單需要的欄位 */
    public static AssigneeResponse from(Agent agent) {
        return new AssigneeResponse(
                agent.getAgentId(),
                agent.getName()
        );
    }
}
