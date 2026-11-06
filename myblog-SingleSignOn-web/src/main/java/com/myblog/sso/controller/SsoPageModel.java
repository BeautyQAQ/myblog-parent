package com.myblog.sso.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.net.URI;
import java.util.Objects;

/** Shared links and anonymous form CSRF for the server-rendered SSO pages. */
@ControllerAdvice
public class SsoPageModel {
    @Value("${portal.public-base-url:http://localhost:8082}")
    private String portalBaseUrl;

    @ModelAttribute
    public void addPageLinks(Model model, HttpServletRequest request) {
        model.addAttribute("portalBaseUrl", portalBaseUrl.replaceAll("/+$", ""));
        if ("GET".equals(request.getMethod()) && request.getServletPath().startsWith("/page/")) {
            model.addAttribute("csrfToken", SessionFormSecurity.token(request));
        } else if ("GET".equals(request.getMethod()) && request.getRequestURI().contains("/page/")) {
            model.addAttribute("csrfToken", SessionFormSecurity.token(request));
        }
    }

    static String loginDestination(String portalBaseUrl, String managerBaseUrl, String redirect) {
        URI portal = URI.create(portalBaseUrl.replaceAll("/+$", "") + "/");
        if (redirect == null || redirect.isBlank()) {
            return portal.toString();
        }
        try {
            URI destination = portal.resolve(redirect).normalize();
            if (allowed(portal, destination) || allowed(URI.create(managerBaseUrl.replaceAll("/+$", "") + "/"), destination)) {
                return destination.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid return addresses use the configured portal home.
        }
        return portal.toString();
    }

    private static boolean allowed(URI base, URI destination) {
        if (destination.getScheme() == null || destination.getHost() == null
                || !("http".equalsIgnoreCase(destination.getScheme()) || "https".equalsIgnoreCase(destination.getScheme()))
                || !destination.getScheme().equalsIgnoreCase(base.getScheme())
                || !destination.getHost().equalsIgnoreCase(base.getHost())
                || destination.getPort() != base.getPort()
                || !Objects.equals(destination.getRawUserInfo(), base.getRawUserInfo())) {
            return false;
        }
        String path = destination.getPath();
        if (path == null || path.contains("\\") || path.contains(";") || path.contains("//")) {
            return false;
        }
        for (String part : path.split("/")) {
            if (".".equals(part) || "..".equals(part)) {
                return false;
            }
        }
        String prefix = base.getPath();
        return path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1));
    }
}
