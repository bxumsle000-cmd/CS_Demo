package com.poz.cs_demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登入 / 認證相關 API。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 登入 */
    @PostMapping("/login")
    public void login() {
    }

    /** 登出 */
    @PostMapping("/logout")
    public void logout() {
    }

    /** 取得目前登入者資料 */
    @GetMapping("/me")
    public void me() {
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
