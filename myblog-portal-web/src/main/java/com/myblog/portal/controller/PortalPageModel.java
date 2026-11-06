package com.myblog.portal.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = PortalArticleController.class)
public class PortalPageModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortalPageModel.class);
    private final TokenService tokenService;
    private final String ssoPublicBaseUrl;
    private final String managerPublicBaseUrl;

    public PortalPageModel(TokenService tokenService,
                           @Value("${sso.public-base-url:http://localhost:8086}") String ssoPublicBaseUrl,
                           @Value("${manager.public-base-url:http://localhost:8084}") String managerPublicBaseUrl) {
        this.tokenService = tokenService;
        this.ssoPublicBaseUrl = ssoPublicBaseUrl.replaceAll("/+$", "");
        this.managerPublicBaseUrl = managerPublicBaseUrl.replaceAll("/+$", "");
    }

    @ModelAttribute
    public void populate(Model model, HttpServletRequest request) {
        model.addAttribute("ssoPublicBaseUrl", ssoPublicBaseUrl);
        model.addAttribute("managerPublicBaseUrl", managerPublicBaseUrl);
        model.addAttribute("csrfToken", "");
        model.addAttribute("authenticationUnavailable", false);
        String token = SessionSecurity.readToken(request);
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Result result = tokenService.getUserByToken(token);
            if (result != null && Integer.valueOf(200).equals(result.getStatus())
                    && result.getData() instanceof TbUser user) {
                model.addAttribute("currentUser", user);
                model.addAttribute("csrfToken", SessionSecurity.csrfToken(token));
            }
        } catch (RuntimeException e) {
            model.addAttribute("authenticationUnavailable", true);
            // Account service failures must not prevent visitors from reading public articles.
            LOGGER.warn("Unable to load the current portal user");
        }
    }
}
