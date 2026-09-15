package com.poz.cs_demo.dto.ticket;

import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;

import java.time.LocalDateTime;

/**
 * 工單列表每一列的資料，欄位對應歷史紀錄畫面的表格欄位（不含建立時間）。
 *
 * @param ticketNo     單號，對外編號如 TK-000001
 * @param customerName 姓名，未提供時為 null
 * @param contactPhone 電話，未提供時為 null
 * @param status       狀態
 * @param assigneeId   負責客服代號，例如 CSC00001
 * @param updatedAt    最後更新時間；畫面上顯示「—」的判斷交給前端
 */
public record SearchTicketResponse(
        String ticketNo,
        String customerName,
        String contactPhone,
        TicketStatus status,
        String assigneeId,
        LocalDateTime updatedAt
) {
    /** 從 Entity 轉成 Response，只挑列表需要的欄位 */
    public static SearchTicketResponse from(Ticket ticket) {
        return new SearchTicketResponse(
                ticket.getTicketNo(),
                ticket.getCustomerName(),
                ticket.getContactPhone(),
                ticket.getStatus(),
                ticket.getAssignee().getAgentId(),
                ticket.getUpdatedAt()
        );
    }
}
