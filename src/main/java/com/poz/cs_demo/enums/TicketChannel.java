package com.poz.cs_demo.enums;

/**
 * 派單來源，對應 tickets.channel 的 CHECK 白名單。
 * PHONE = 通話工作台在通話中建立，AGENT = 客服從「＋ 新增派件」手動建立。
 * 資料庫定序不分大小寫，這裡統一用全大寫，由後端保證一致。
 */
public enum TicketChannel {
    PHONE,
    AGENT
}
