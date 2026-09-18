package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.agent.ChangePasswordRequest;
import com.poz.cs_demo.dto.agent.RegisterRequest;
import com.poz.cs_demo.dto.agent.UpdateAgentStatusRequest;
import com.poz.cs_demo.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客服人員相關 API。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentService agentService;
    /** 變更自己的工作狀態（ON_CALL 由系統控制，不接受手動設定） */
    @PutMapping("/me/status")
    public void updateMyStatus(@RequestBody UpdateAgentStatusRequest request) {
        agentService.updateStatus(request);
    }

    /** 建立客服帳號 */
    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) {
        agentService.register(request);
    }

    /** 修改密碼 */
    @PutMapping("/password")
    public void changePassword(@RequestBody ChangePasswordRequest request) {
        agentService.changePassword(request);
    }
}
