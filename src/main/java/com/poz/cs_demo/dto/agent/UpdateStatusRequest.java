package com.poz.cs_demo.dto.agent;

import com.poz.cs_demo.enums.AgentStatus;

public record UpdateStatusRequest(
    AgentStatus status
) {
}
