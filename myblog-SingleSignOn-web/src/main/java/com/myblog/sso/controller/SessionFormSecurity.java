package com.myblog.sso.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

final class SessionFormSecurity {
    private static final String ATTRIBUTE = SessionFormSecurity.class.getName() + ".csrf";

    private SessionFormSecurity() {}

    static String token(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(ATTRIBUTE);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(ATTRIBUTE, token);
        }
        return token;
    }

    static boolean valid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(ATTRIBUTE);
        String submitted = request.getHeader("X-CSRF-Token");
        if (submitted == null) {
            submitted = request.getParameter("_csrf");
        }
        return expected != null && submitted != null && submitted.length() == expected.length()
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                submitted.getBytes(StandardCharsets.US_ASCII));
    }
}
