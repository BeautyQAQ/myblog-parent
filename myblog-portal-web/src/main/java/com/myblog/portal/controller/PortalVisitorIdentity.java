package com.myblog.portal.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stable, pseudonymous reading identity; never derives an account ID from client input. */
final class PortalVisitorIdentity {
    static final String COOKIE_NAME = "MYBLOG_VISITOR";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private PortalVisitorIdentity() {}

    static String key(HttpServletRequest request, HttpServletResponse response, Long userId) {
        if (userId != null) return hash("user:" + userId);
        String visitor = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
                        && UUID_PATTERN.matcher(cookie.getValue()).matches()) {
                    visitor = cookie.getValue();
                    break;
                }
            }
        }
        if (visitor == null) {
            visitor = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(COOKIE_NAME, visitor);
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setAttribute("SameSite", "Lax");
            cookie.setPath(request.getContextPath().isEmpty() ? "/" : request.getContextPath());
            cookie.setMaxAge(365 * 24 * 60 * 60);
            response.addCookie(cookie);
        }
        return hash("visitor:" + visitor);
    }

    private static String hash(String identity) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
