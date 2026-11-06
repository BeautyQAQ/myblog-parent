package com.myblog.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ManagerPageModel {
    @ModelAttribute
    public void sessionModel(HttpServletRequest request, Model model) {
        for (String name : new String[]{"currentUser", "csrfToken", "ssoLoginUrl"}) {
            model.addAttribute(name, request.getAttribute(name));
        }
    }
}
