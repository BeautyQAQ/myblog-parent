package com.myblog.common.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Shared host-only authentication cookie and token-bound CSRF protection. */
public final class SessionSecurity {
    public static final String TOKEN_COOKIE = "MYBLOG_TOKEN";

    private SessionSecurity() {}

    public static String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_COOKIE.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    return value == null || value.isBlank() || value.length() > 200 ? null : value;
                }
            }
        }
        return null;
    }

    public static String csrfToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("myblog-csrf:" + token).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean validCsrf(String token, String submitted) {
        return token != null && !token.isBlank() && submitted != null && submitted.length() == 64
                && MessageDigest.isEqual(csrfToken(token).getBytes(StandardCharsets.US_ASCII),
                submitted.getBytes(StandardCharsets.US_ASCII));
    }

    public static void setTokenCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        Cookie cookie = cookie(request, token);
        response.addCookie(cookie);
    }

    public static void clearTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = cookie(request, "");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private static Cookie cookie(HttpServletRequest request, String token) {
        Cookie cookie = new Cookie(TOKEN_COOKIE, token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
}
