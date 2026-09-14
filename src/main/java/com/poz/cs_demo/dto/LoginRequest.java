package com.poz.cs_demo.dto;

/**
 *
 * @param agentId
 * @param password
 */
public record LoginRequest(
        String agentId,
        String password
) {
}
