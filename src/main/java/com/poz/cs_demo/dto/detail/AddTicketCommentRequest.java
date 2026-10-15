package com.poz.cs_demo.dto.detail;

import jakarta.validation.constraints.NotBlank;

/**
 * 工單詳情頁「新增處理記錄」的請求。
 *
 * @param content 留言內容，對應 ticket_comments.content NVARCHAR(MAX)
 */
public record AddTicketCommentRequest(
    @NotBlank(message = "不可為空")
    String content
) {
}
