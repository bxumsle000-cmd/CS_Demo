package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.ticket.SearchTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketResponse;
import com.poz.cs_demo.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工單相關 API。
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    /**
     * 查詢工單列表。
     * 所有 query 參數（單號、姓名、電話、狀態、客服、起訖日期、page、size）
     * 由 Spring 自動綁進 SearchTicketRequest，皆為選填。
     */
    @GetMapping
    public PagedModel<SearchTicketResponse> search(SearchTicketRequest request) {
        return new PagedModel<>(ticketService.search(request));
    }

    /** 建立工單 */
    @PostMapping
    public void create() {
    }
}
