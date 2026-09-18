package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.ticket.CreateTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketRequest;
import com.poz.cs_demo.dto.ticket.SearchTicketResponse;
import com.poz.cs_demo.enums.TicketChannel;
import com.poz.cs_demo.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

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
    public Page<SearchTicketResponse> search(SearchTicketRequest request) {
        return ticketService.search(request);
    }

    /** 建立工單 from call */
    @PostMapping("/from-call")
    public void createFromCall(@RequestBody CreateTicketRequest request) {
        ticketService.createTicket(request, TicketChannel.PHONE);
    }

    /** 建立工單 from agent*/
    @PostMapping("/from-agent")
    public void createFromAgent(@RequestBody CreateTicketRequest request) {
        ticketService.createTicket(request, TicketChannel.AGENT);
    }
}
