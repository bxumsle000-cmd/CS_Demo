package com.poz.cs_demo.dto.agent;

import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.enums.AgentStatus;

/**
 * 回給前端的客服資料，刻意不含密碼雜湊。
 *
 * @param agentId 客服代號，例如 CSC00001
 * @param name    客服姓名
 * @param status  目前工作狀態
 */

public record AgentResponse(
    String agentId,
    String name,
    AgentStatus status
) {
    /** 從 Entity 轉成 Response，只挑可以對外的欄位 */
    public static AgentResponse from(Agent agent){
        return new AgentResponse(
                agent.getAgentId(),
                agent.getName(),
                agent.getStatus()
        );
    }
}
