package com.poz.cs_demo.dto.ticket;

import com.poz.cs_demo.enums.TicketStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工單列表的搜尋條件，欄位對應畫面上方的篩選列，外加分頁參數。
 * <p>
 * 所有篩選欄位都是選填，null 代表該條件為「全部」、不過濾；
 * 空字串會在建構時統一轉成 null，Repository 只要判斷 null 即可。
 * 上方的狀態頁籤（全部 / 已處理 / 處理中 / 等待客戶回覆）與篩選列的「狀態」下拉
 * 都對應同一個 {@code status} 欄位，切換頁籤時其他條件照舊帶入，不會重置。
 *
 * @param ticketNo     單號，對外編號如 TK-000001，模糊比對
 * @param customerName 姓名，模糊比對
 * @param contactPhone 電話，模糊比對
 * @param status       狀態；null = 全部
 * @param assigneeId   客服代號，例如 CSC00001；null = 全部
 * @param updatedFrom  起始日期（含），依更新時間過濾；null = 不限起始
 * @param updatedTo    結束日期（含），依更新時間過濾；null = 不限結束
 * @param page         頁碼，從 0 開始；沒帶預設 0
 * @param size         每頁筆數；沒帶預設 10
 */
public record SearchTicketRequest(
        String ticketNo,
        String customerName,
        String contactPhone,
        TicketStatus status,
        String assigneeId,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate updatedFrom,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate updatedTo,
        Integer page,
        Integer size
) {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    /** 建構時統一整理輸入：空白字串視為沒填、分頁沒帶就用預設值 */
    public SearchTicketRequest {
        ticketNo = blankToNull(ticketNo);
        customerName = blankToNull(customerName);
        contactPhone = blankToNull(contactPhone);
        assigneeId = blankToNull(assigneeId);
        page = (page == null || page < 0) ? DEFAULT_PAGE : page;
        size = (size == null || size < 10) ? DEFAULT_SIZE : size;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 起始日 00:00:00（含）；沒填回 null */
    public LocalDateTime updatedFromInclusive() {
        return updatedFrom == null ? null : updatedFrom.atStartOfDay();
    }

    /** 結束日的隔天 00:00:00（不含），這樣結束日整天都會算在內；沒填回 null */
    public LocalDateTime updatedToExclusive() {
        return updatedTo == null ? null : updatedTo.plusDays(1).atStartOfDay();
    }

    /** 轉成 Spring Data 的分頁物件，固定依建立時間新到舊排序 */
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}
