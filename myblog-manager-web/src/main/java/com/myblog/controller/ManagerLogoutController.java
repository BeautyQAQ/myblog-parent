package com.myblog.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ManagerLogoutController {
    private final TokenService tokenService;
    public ManagerLogoutController(TokenService tokenService) { this.tokenService = tokenService; }

    @PostMapping("/auth/logout")
    @ResponseBody
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        Result result = tokenService.logout(SessionSecurity.readToken(request));
        if (result == null || !Integer.valueOf(200).equals(result.getStatus())) {
            response.setStatus(503);
            return Result.build(503, "退出失败，请稍后重试");
        }
        SessionSecurity.clearTokenCookie(request, response);
        return result;
    }
}
