package com.poz.cs_demo.dto.detail;

import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketChannel;
import com.poz.cs_demo.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工單詳情頁的工單內容，欄位對應畫面左上的主旨／描述與右側的資訊欄。
 * 處理記錄一併包在 comments 裡，前端改完狀態後不必再打一次 GET /comments。
 *
 * @param ticketNo     單號，對外編號如 TK-000001
 * @param title        主旨
 * @param description  問題描述
 * @param status       狀態
 * @param assigneeId   負責客服代號，例如 CSC00001
 * @param category     問題分類
 * @param channel      進線管道
 * @param customerName 客戶姓名，未提供時為 null
 * @param contactPhone 聯絡電話，未提供時為 null；畫面上顯示「—」的判斷交給前端
 * @param createdAt    建立時間
 * @param comments     處理記錄列表，舊到新排序
 */
public record TicketDetailResponse(
        String ticketNo,
        String title,
        String description,
        TicketStatus status,
        String assigneeId,
        String category,
        TicketChannel channel,
        String customerName,
        String contactPhone,
        LocalDateTime createdAt,
        List<TicketCommentResponse> comments
) {
    /** 從 Entity 轉成 Response。Ticket 上沒有 comments 關聯，留言由呼叫端另外查好傳進來 */
    public static TicketDetailResponse from(Ticket ticket, List<TicketCommentResponse> comments) {
        return new TicketDetailResponse(
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAssignee().getAgentId(),
                ticket.getCategory(),
                ticket.getChannel(),
                ticket.getCustomerName(),
                ticket.getContactPhone(),
                ticket.getCreatedAt(),
                comments
        );
    }
}
