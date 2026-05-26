package com.carp.ai.langchain4j.controller;

import com.carp.ai.langchain4j.bean.AuthRequest;
import com.carp.ai.langchain4j.bean.AuthUserView;
import com.carp.ai.langchain4j.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "轻量用户认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Operation(summary = "获取默认用户")
    @GetMapping("/default-user")
    public AuthUserView defaultUser() {
        return authService.getDefaultUser();
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public AuthUserView register(@RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public AuthUserView login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(IllegalArgumentException e) {
        return e.getMessage();
    }
}
