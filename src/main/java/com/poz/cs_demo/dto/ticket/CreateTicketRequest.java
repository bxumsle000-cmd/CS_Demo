package com.poz.cs_demo.dto.ticket;

import com.poz.cs_demo.enums.TicketStatus;

/**
 * 通話工作台「建立工單並結束通話」的請求，欄位對應畫面上的「本次通話工單」表單。
 * <p>
 * 不從 body 帶的欄位：
 * <ul>
 *   <li>ticketNo、createdAt：資料庫產生</li>
 *   <li>channel：由通話工作台建立，Service 固定填 PHONE</li>
 * </ul>
 *
 * @param title        主旨（必填），對應 tickets.title NVARCHAR(50)
 * @param customerName 通話中向客戶確認的姓名，未識別可為 null
 * @param contactPhone 客戶提供的聯絡電話，可按「帶入進線號碼」直接帶入；未提供可為 null
 * @param category     問題分類（必填），例如 帳號問題 / 付款、發票
 * @param assigneeId   轉派對象的客服代號；null 表示不轉派，由目前登入者自己負責
 * @param description  通話摘要（邊講邊記），可為 null
 * @param status       通話結果：已解決 = RESOLVED、需再追蹤 = IN_PROGRESS、待客戶回覆 = PENDING
 */
public record CreateTicketRequest(
        String title,
        String customerName,
        String contactPhone,
        String category,
        String assigneeId,
        String description,
        TicketStatus status
) {
}
