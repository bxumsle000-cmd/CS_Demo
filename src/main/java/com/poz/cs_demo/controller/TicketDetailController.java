package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.detail.AddTicketCommentRequest;
import com.poz.cs_demo.dto.detail.TicketDetailResponse;
import com.poz.cs_demo.dto.detail.UpdateTicketAssigneeRequest;
import com.poz.cs_demo.dto.detail.UpdateTicketStatusRequest;
import com.poz.cs_demo.repository.TicketCommentRepository;
import com.poz.cs_demo.service.TicketDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 工單詳情頁相關 API，對應從歷史紀錄點進單一工單後的畫面。
 * 路徑一律以單號（ticketNo，例如 TK-000001）定位工單。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tickets/{ticketNo}")
public class TicketDetailController {
    private  final TicketDetailService ticketDetailService;
    /** 取得工單內容（主旨、描述、狀態、分類、進線管道、客服、客戶、建立時間） */
    @GetMapping
    public  TicketDetailResponse detail(@PathVariable String ticketNo) {
        return ticketDetailService.getDetail(ticketNo);
    }

    /** 變更工單狀態（標記為「待客戶回覆」/「已解決」） */
    @PutMapping("/status")
    public TicketDetailResponse updateStatus(@PathVariable String ticketNo,
                                             @RequestBody UpdateTicketStatusRequest request) {
        return ticketDetailService.updateStatus(ticketNo,request);
    }

    /** 轉派給其他客服 */
    @PutMapping("/assignee")
    public TicketDetailResponse reassign(@PathVariable String ticketNo,
                                         @RequestBody UpdateTicketAssigneeRequest request) {
        return ticketDetailService.assign(ticketNo , request);
    }

    /** 新增處理記錄 */
    @PostMapping("/comments")
    public TicketDetailResponse addComment(@PathVariable String ticketNo,
                           @RequestBody AddTicketCommentRequest request) {
        return ticketDetailService.addComment(ticketNo,request);
    }
}
