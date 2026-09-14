package com.poz.cs_demo.dto;

import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.enums.AgentStatus;

/**
 *
 * @param agentId
 * @param name
 * @param status
 */

public record AgentResponse(
    String agentId,
    String name,
    AgentStatus status
) {
    public static AgentResponse from(Agent agent){
        return new AgentResponse(
                agent.getAgentId(),
                agent.getName(),
                agent.getStatus()
        );
    }
}
