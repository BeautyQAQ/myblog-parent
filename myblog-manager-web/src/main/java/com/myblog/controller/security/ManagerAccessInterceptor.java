package com.myblog.controller.security;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.JsonUtils;
import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** All management requests resolve their identity through SSO, including uploads. */
@Component
public class ManagerAccessInterceptor implements HandlerInterceptor {
    private final TokenService tokenService;
    private final String ssoBaseUrl;
    private final String managerBaseUrl;

    public ManagerAccessInterceptor(TokenService tokenService,
            @Value("${sso.public-base-url}") String ssoBaseUrl,
            @Value("${manager.public-base-url:http://localhost:8084}") String managerBaseUrl) {
        this.tokenService = tokenService;
        this.ssoBaseUrl = ssoBaseUrl.replaceAll("/+$", "");
        this.managerBaseUrl = managerBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean managementPage = "/".equals(path) || "/article_admin".equals(path) || "/comment_admin".equals(path);
        String destination = managerBaseUrl + (managementPage ? path : "/");
        String loginUrl = ssoBaseUrl + "/page/login?redirect="
                + URLEncoder.encode(destination, StandardCharsets.UTF_8);
        request.setAttribute("ssoLoginUrl", loginUrl);
        String token = SessionSecurity.readToken(request);
        TbUser user = null;
        if (token != null && !token.isBlank()) {
            try {
                Result result = tokenService.getUserByToken(token);
                if (result != null && Integer.valueOf(200).equals(result.getStatus())
                        && result.getData() instanceof TbUser found && found.getId() != null) {
                    user = found;
                }
            } catch (RuntimeException e) {
                reject(response, 503, "登录服务暂时不可用，请稍后重试");
                return false;
            }
        }
        if (user == null) {
            if ("GET".equals(request.getMethod()) && managementPage) {
                response.sendRedirect(loginUrl);
            } else {
                reject(response, 401, "请先登录");
            }
            return false;
        }
        if (!"ADMIN".equals(user.getRole())) {
            reject(response, 403, "当前账号没有管理员权限");
            return false;
        }
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())
                && !"OPTIONS".equals(request.getMethod())) {
            String csrf = request.getHeader("X-CSRF-Token");
            if (csrf == null) csrf = request.getParameter("_csrf");
            if (!SessionSecurity.validCsrf(token, csrf)) {
                reject(response, 403, "页面验证已失效，请刷新后重试");
                return false;
            }
        }
        request.setAttribute("currentUser", user);
        request.setAttribute("csrfToken", SessionSecurity.csrfToken(token));
        return true;
    }

    private static void reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.objectToJson(Result.build(status, message)));
    }
}
