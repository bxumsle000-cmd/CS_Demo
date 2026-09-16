package com.poz.cs_demo.dto.detail;

import com.poz.cs_demo.entity.Agent;
import com.poz.cs_demo.entity.TicketComment;

import java.time.LocalDateTime;

/**
 * 工單詳情頁「處理記錄」列表中的一筆留言。
 *
 * @param commentId 留言流水號
 * @param agentId   留言客服代號；系統事件為 null
 * @param agentName 留言客服姓名；系統事件為 null
 * @param content   留言內容或系統事件描述
 * @param createdAt 建立時間
 */
public record TicketCommentResponse(
        Integer commentId,
        String agentId,
        String agentName,
        String content,
        LocalDateTime createdAt
) {
    /** 從 Entity 轉成 Response。agent 可能為 null（系統事件），要先判斷再取值 */
    public static TicketCommentResponse from(TicketComment comment) {
        Agent agent = comment.getAgent();
        return new TicketCommentResponse(
                comment.getCommentId(),
                agent == null ? null : agent.getAgentId(),
                agent == null ? null : agent.getName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
