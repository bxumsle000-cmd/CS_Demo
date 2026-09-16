package com.poz.cs_demo.dto.followup;

import com.poz.cs_demo.entity.FollowUp;
import com.poz.cs_demo.entity.Ticket;
import com.poz.cs_demo.enums.TicketStatus;

import java.time.LocalDateTime;

/**
 * 行事曆上的一筆回電安排，欄位對應畫面右邊「當天案件詳情」那一列。
 * <p>
 * 回傳的是扁平的清單，不在後端依日期分組：
 * 月曆格子上的件數角標，由前端拿 followUpAt 自己 group by 日期算出來。
 *
 * @param followUpId   安排的識別碼，刪除（右上角的 ×）時要用
 * @param followUpAt   排定的回電時間，前端用它決定落在哪一格、顯示 10:30
 * @param ticketNo     單號，對外編號如 TK-000001
 * @param title        工單標題
 * @param customerName 客戶姓名，未提供時為 null
 * @param contactPhone 聯絡電話，未提供時為 null
 * @param status       工單狀態
 * @param note         個人備註，只有主人看得到
 */
public record FollowUpGetListResponse(
        Integer followUpId,
        LocalDateTime followUpAt,
        String ticketNo,
        String title,
        String customerName,
        String contactPhone,
        TicketStatus status,
        String note
) {
    /** 從 Entity 轉成 Response，只挑行事曆需要的欄位 */
    public static FollowUpGetListResponse from(FollowUp followUp) {
        Ticket ticket = followUp.getTicket();
        return new FollowUpGetListResponse(
                followUp.getFollowUpId(),
                followUp.getFollowUpAt(),
                ticket.getTicketNo(),
                ticket.getTitle(),
                ticket.getCustomerName(),
                ticket.getContactPhone(),
                ticket.getStatus(),
                followUp.getNote()
        );
    }
}
