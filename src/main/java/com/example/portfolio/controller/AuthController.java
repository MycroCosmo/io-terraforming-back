package com.example.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.portfolio.model.Admin;
import com.example.portfolio.service.AdminService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminService adminService;

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/login-success")
    public ResponseEntity<String> loginSuccess() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authentication.getName());
    }

    @PostMapping("/signup")
    public String signUp(@RequestBody Admin admin) {
        adminService.signUpAdmin(admin);
        return "회원가입 성공";
    }
}
