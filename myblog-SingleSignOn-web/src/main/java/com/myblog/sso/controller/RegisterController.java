package com.myblog.sso.controller;

import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.RegisterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RegisterController {
    @Autowired
    private RegisterService registerService;

    @RequestMapping("/page/register")
    public String showRegister() {
        return "register";
    }

    @RequestMapping("/user/check/{param}/{type}")
    @ResponseBody
    public Result checkData(@PathVariable String param, @PathVariable Integer type) {
        return registerService.checkData(param, type);
    }

    @RequestMapping(value="/user/register", method=RequestMethod.POST)
    @ResponseBody
    public Result register(@RequestParam String username, @RequestParam String password,
                           @RequestParam(required=false) String phone, @RequestParam(required=false) String email,
                           HttpServletRequest request, HttpServletResponse response) {
        if (!SessionFormSecurity.valid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return Result.build(403, "页面已过期，请刷新后重试");
        }
        TbUser user = new TbUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole(TbUser.ROLE_USER);
        return registerService.register(user);
    }
}
