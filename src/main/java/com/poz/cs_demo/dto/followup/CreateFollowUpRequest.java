package com.poz.cs_demo.dto.followup;

import java.time.LocalDateTime;

/**
 * 行事曆「加入案件到這天」的請求，對應畫面右側的單號下拉 + 時間選擇器。
 * <p>
 * 主人（agent）由 CurrentAgent 取得，不從 body 帶。
 * 日期和時間不拆成兩個欄位：Entity 存的本來就只有一個 followUpAt，
 * 由前端把選到的日期與時間拼成 ISO 格式（例如 2026-09-14T09:00:00）送過來，
 * Jackson 預設就能解析成 LocalDateTime，Service 不必再自己組合。
 * <p>
 * 同一張工單可以排任意多筆安排，時間重複也不限制（見 V2 migration）。
 *
 * @param ticketNo   單號，對外編號如 TK-000001
 * @param followUpAt 排定的回電時間，ISO 格式 yyyy-MM-ddTHH:mm:ss
 * @param note       個人備註，只有主人看得到；可為 null，對應 follow_ups.note NVARCHAR(200)
 */
public record CreateFollowUpRequest(
        String ticketNo,
        LocalDateTime followUpAt,
        String note
) {
}
