package com.myblog.sso.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.sso.service.LoginService;
import com.myblog.sso.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Autowired
    private TokenService tokenService;
    @Value("${portal.public-base-url:http://localhost:8082}")
    private String portalBaseUrl;
    @Value("${manager.public-base-url:http://localhost:8084}")
    private String managerBaseUrl;

    @RequestMapping("/page/login")
    public String showLogin(String redirect, Model model) {
        model.addAttribute("redirect", SsoPageModel.loginDestination(portalBaseUrl, managerBaseUrl, redirect));
        return "login";
    }

    @RequestMapping(value="/user/login", method=RequestMethod.POST)
    @ResponseBody
    public Result login(String username, String password,
                        HttpServletRequest request, HttpServletResponse response) {
        if (!SessionFormSecurity.valid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.build(403, "页面已过期，请刷新后重试");
        }
        Result result = loginService.userLogin(username, password);
        if (result.getStatus() == 200) {
            tokenService.logout(SessionSecurity.readToken(request));
            SessionSecurity.setTokenCookie(request, response, result.getData().toString());
            request.getSession(false).invalidate();
        }
        return result;
    }
}
