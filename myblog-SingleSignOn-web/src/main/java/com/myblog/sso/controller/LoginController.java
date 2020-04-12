package com.myblog.sso.controller;

import com.myblog.common.utils.CookieUtils;
import com.myblog.common.utils.Result;
import com.myblog.sso.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LoginController {
    @Autowired
    private LoginService loginService;
    @Value("${TOKEN_KEY}")
    private String TOKEN_KEY;

    @RequestMapping("/page/login")
    public String showLogin(String redirect, Model model) {
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @RequestMapping(value="/user/login", method= RequestMethod.POST)
    @ResponseBody
    public Result login(String username, String password,
                        HttpServletRequest request, HttpServletResponse response) {
        Result Result = loginService.userLogin(username, password);
        //判断是否登录成功
        if(Result.getStatus() == 200) {
            String token = Result.getData().toString();
            //如果登录成功需要把token写入cookie
            CookieUtils.setCookie(request, response, TOKEN_KEY, token);
        }
        //返回结果
        return Result;
    }
}
