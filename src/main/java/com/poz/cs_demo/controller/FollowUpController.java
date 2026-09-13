package com.poz.cs_demo.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 行事曆回電安排相關 API，只操作目前登入者自己的安排。
 */
@RestController
@RequestMapping("/api/follow-ups")
public class FollowUpController {

    /** 查詢區間內的回電安排（from、to 為 query 參數） */
    @GetMapping
    public void list() {
    }

    /** 加入案件到某一天（同一張工單已有安排則覆蓋） */
    @PostMapping
    public void create() {
    }

    /** 刪除回電安排 */
    @DeleteMapping("/{followUpId}")
    public void delete(@PathVariable Integer followUpId) {
    }
}
