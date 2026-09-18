package com.poz.cs_demo.controller;

import com.poz.cs_demo.dto.agent.AgentResponse;
import com.poz.cs_demo.dto.auth.LoginRequest;
import com.poz.cs_demo.dto.auth.LoginResponse;
import com.poz.cs_demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登入 / 認證相關 API。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    /** 登入 */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 登出 */
    @PostMapping("/logout")
    public void logout() {
        authService.logout();
    }

    /** 取得目前登入者資料 */
    @GetMapping("/me")
    public AgentResponse me() {
        return authService.me();
    }


}
