package com.poz.cs_demo.dto;

import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.repository.AgentRepository;

/**
 *
 * @param agentId
 * @param token
 */
public record LoginResponse (
        String agentId,
        String token
){
}
