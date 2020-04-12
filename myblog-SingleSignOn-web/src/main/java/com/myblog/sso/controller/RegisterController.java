package com.myblog.sso.controller;

import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
        Result Result = registerService.checkData(param, type);
        return Result;
    }

    @RequestMapping(value="/user/register", method= RequestMethod.POST)
    @ResponseBody
    public Result register(TbUser user) {
        System.out.println("==============userId:"+user.getId());
        System.out.println("==============userEmail:"+user.getEmail());
        System.out.println("==============userphone:"+user.getPhone());
        System.out.println("==============useruser:"+user.getUsername());
        System.out.println("==============userpwd:"+user.getPassword());
        Result Result = registerService.register(user);
        System.out.println(Result.getMsg());
        return Result;
    }
}
