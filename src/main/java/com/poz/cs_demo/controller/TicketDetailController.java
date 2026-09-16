package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.detail.TicketDetailResponse;
import com.poz.cs_demo.service.TicketDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工單詳情頁相關 API，對應從歷史紀錄點進單一工單後的畫面。
 * 路徑一律以單號（ticketNo，例如 TK-000001）定位工單。
 */
@RestController
@RequestMapping("/api/tickets/{ticketNo}")
public class TicketDetailController {

    /** 取得工單內容（主旨、描述、狀態、分類、進線管道、客服、客戶、建立時間） */
    @GetMapping
    public void detail(@PathVariable String ticketNo) {
    }

    /** 變更工單狀態（標記為「待客戶回覆」/「已解決」） */
    @PutMapping("/status")
    public void updateStatus(@PathVariable String ticketNo) {
    }

    /** 轉派給其他客服 */
    @PutMapping("/assignee")
    public void reassign(@PathVariable String ticketNo) {
    }


    /** 新增處理記錄 */
    @PostMapping("/comments")
    public void addComment(@PathVariable String ticketNo) {
    }
}
