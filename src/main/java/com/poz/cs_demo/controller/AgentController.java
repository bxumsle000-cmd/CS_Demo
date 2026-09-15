package com.poz.cs_demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服人員相關 API。
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    /** 變更自己的工作狀態（ON_CALL 由系統控制，不接受手動設定） */
    @PutMapping("/me/status")
    public void updateMyStatus() {
    }

    /** 建立客服帳號 */
    @PostMapping("/register")
    public void register() {
    }

    /** 修改密碼 */
    @PutMapping("/password")
    public void changePassword() {
    }
}
