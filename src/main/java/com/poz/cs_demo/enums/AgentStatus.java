package com.poz.cs_demo.enums;

/**
 * 客服目前工作狀態，對應 agents.status 的 CHECK 白名單。
 * ON_CALL 由系統在通話事件時設定，OFFLINE 由登出設定，其餘為客服手動選擇。
 */
public enum AgentStatus {
    ONLINE,
    ON_CALL,
    BREAK,
    RESTROOM,
    LUNCH,
    MEETING,
    OFFLINE
}
