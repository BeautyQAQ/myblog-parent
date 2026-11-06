package com.myblog.sso.controller;

import com.myblog.common.utils.JsonUtils;
import com.myblog.common.security.SessionSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.myblog.common.utils.Result;
import com.myblog.sso.service.TokenService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.regex.Pattern;

@Controller
public class TokenController {
    private static final Pattern JSONP_CALLBACK = Pattern.compile(
            "[a-zA-Z_$][a-zA-Z0-9_$]*(?:\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*");

    @Autowired
    private TokenService tokenService;

    @PostMapping("/user/logout")
    @ResponseBody
    public Result logout(HttpServletRequest request, HttpServletResponse response) {
        String token = SessionSecurity.readToken(request);
        String submitted = request.getHeader("X-CSRF-Token");
        if (submitted == null) {
            submitted = request.getParameter("_csrf");
        }
        if (!SessionSecurity.validCsrf(token, submitted)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.build(403, "请求校验失败，请刷新页面后重试");
        }
        Result result = tokenService.logout(token);
        SessionSecurity.clearTokenCookie(request, response);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return result;
    }

    @RequestMapping(value="/user/token/{token}")
    public ResponseEntity<?> getUserByToken(@PathVariable String token,
                                           @RequestParam(required = false) String callback) {
        if (StringUtils.isNotBlank(callback)) {
            if (!JSONP_CALLBACK.matcher(callback).matches()) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body(Result.build(400, "无效的 JSONP 回调名称"));
            }
            Result result = tokenService.getUserByToken(token);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/javascript;charset=UTF-8"))
                    .body(callback + "(" + JsonUtils.objectToJson(result) + ");");
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(tokenService.getUserByToken(token));
    }
}
