package com.myblog.portal.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PortalSessionController {
    private final TokenService tokenService;

    public PortalSessionController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/auth/logout")
    @ResponseBody
    public ResponseEntity<Result> logout(HttpServletRequest request, HttpServletResponse response,
                                         @RequestHeader(value = "X-CSRF-Token", required = false) String csrf) {
        String token = SessionSecurity.readToken(request);
        if (token == null || token.isBlank()) {
            SessionSecurity.clearTokenCookie(request, response);
            return ResponseEntity.ok(Result.ok());
        }
        if (!SessionSecurity.validCsrf(token, csrf)) {
            return ResponseEntity.status(403).body(Result.build(403, "请求验证失败，请刷新页面后重试"));
        }
        try {
            Result result = tokenService.logout(token);
            if (result == null || !Integer.valueOf(200).equals(result.getStatus())) {
                return ResponseEntity.status(503).body(Result.build(503, "退出失败，请稍后重试"));
            }
            SessionSecurity.clearTokenCookie(request, response);
            return ResponseEntity.ok(Result.ok());
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Result.build(503, "退出失败，请稍后重试"));
        }
    }
}
