package com.myblog.sso.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.net.URI;
import java.util.Objects;

/** Shared links for the server-rendered SSO pages. */
@ControllerAdvice
public class SsoPageModel {
    @Value("${portal.public-base-url:http://localhost:8082}")
    private String portalBaseUrl;

    @ModelAttribute
    public void addPageLinks(Model model) {
        model.addAttribute("portalBaseUrl", portalBaseUrl.replaceAll("/+$", ""));
    }

    static String loginDestination(String portalBaseUrl, String redirect) {
        URI portal = URI.create(portalBaseUrl.replaceAll("/+$", "") + "/");
        if (redirect == null || redirect.isBlank()) {
            return portal.toString();
        }
        try {
            URI destination = portal.resolve(redirect);
            // Login may return to the configured portal, but never to a user-supplied host or script.
            if (destination.getScheme().equalsIgnoreCase(portal.getScheme())
                    && destination.getHost() != null
                    && destination.getHost().equalsIgnoreCase(portal.getHost())
                    && destination.getPort() == portal.getPort()
                    && Objects.equals(destination.getRawUserInfo(), portal.getRawUserInfo())) {
                return destination.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed destinations use the portal home page.
        }
        return portal.toString();
    }
}
