package com.poz.cs_demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工單相關 API。
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    /**
     * 查詢工單列表（單號、姓名、電話、狀態、客服、時間皆為選填的 query 參數）。
     * page 從 0 開始，size 為每頁筆數。
     */
    @GetMapping
    public void list(@RequestParam(defaultValue = "0") int page,
                     @RequestParam(defaultValue = "10") int size) {
    }

    /** 建立工單 */
    @PostMapping
    public void create() {
    }
}
