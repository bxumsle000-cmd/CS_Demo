package com.poz.cs_demo.dto.followup;

import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;

/**
 * 行事曆「加入案件到這天」下拉選單的一個選項，
 * 畫面上顯示成「TK-316586｜帳號無法登入（處理中）」。
 *
 * @param ticketNo 單號，對外編號如 TK-000001；送出加入時要帶回後端
 * @param title    工單標題
 * @param status   工單狀態，只會是 IN_PROGRESS 或 PENDING
 */
public record AssignableTicketResponse(
        String ticketNo,
        String title,
        TicketStatus status
) {
    /** 從 Entity 轉成 Response，只挑下拉選單需要的欄位 */
    public static AssignableTicketResponse from(Ticket ticket) {
        return new AssignableTicketResponse(
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getStatus()
        );
    }
}
