package com.poz.cs_demo.dto.followup;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 行事曆查詢條件，對應畫面左邊月曆要顯示的日期範圍。
 * <p>
 * 傳的是「日曆實際畫出來的第一格與最後一格」，而不是年月：
 * 9 月的月曆上第一格是 8/30、最後一格是 10/10，這些鄰月格子一樣要顯示件數，
 * 由前端直接指定範圍，後端就不必去猜一週從星期幾開始、要補幾格。
 *
 * @param from 起始日期（含）
 * @param to   結束日期（含）
 */
public record FollowUpGetListRequest(
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to
) {
    /** 起始日 00:00:00（含） */
    public LocalDateTime fromInclusive() {
        return from.atStartOfDay();
    }

    /**
     * 結束日的隔天 00:00:00（不含），這樣結束日整天都會算在內。
     * followUpAt 精度到秒，若寫成「<= 結束日 00:00」，當天 10:30 的安排就會被漏掉。
     */
    public LocalDateTime toExclusive() {
        return to.plusDays(1).atStartOfDay();
    }
}
