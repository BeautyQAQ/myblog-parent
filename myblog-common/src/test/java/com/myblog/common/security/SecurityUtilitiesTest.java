package com.myblog.common.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilitiesTest {
    @Test
    void passwordHashesHaveFreshSaltsAndRejectWrongOrMalformedCredentials() {
        String first = PasswordSecurity.hash("test-password");
        String second = PasswordSecurity.hash("test-password");
        assertNotEquals(first, second);
        assertTrue(PasswordSecurity.matches("test-password", first));
        assertFalse(PasswordSecurity.matches("wrong-password", first));
        assertFalse(PasswordSecurity.matches(null, first));
        assertFalse(PasswordSecurity.matches("test-password", "pbkdf2-sha256$-1$x$x"));
        assertFalse(PasswordSecurity.matches("test-password", "invalid"));
        assertTrue(PasswordSecurity.matches("password", "5f4dcc3b5aa765d61d8327deb882cf99"));
        assertFalse(PasswordSecurity.matches("wrong-password", "5f4dcc3b5aa765d61d8327deb882cf99"));
    }

    @Test
    void csrfIsBoundToTheCurrentTokenAndCannotUseAnEmptyIdentity() {
        String csrf = SessionSecurity.csrfToken("session-a");
        assertTrue(SessionSecurity.validCsrf("session-a", csrf));
        assertFalse(SessionSecurity.validCsrf("session-b", csrf));
        assertFalse(SessionSecurity.validCsrf(null, ""));
        assertFalse(SessionSecurity.validCsrf("session-a", null));
    }

    @Test
    void authCookiesUseHttpsAndExplicitDeletionWithoutADomain() {
        AtomicReference<Cookie> saved = new AtomicReference<>();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isSecure" -> true;
                    case "getCookies" -> saved.get() == null ? null : new Cookie[]{saved.get()};
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if (!"addCookie".equals(method.getName())) throw new UnsupportedOperationException(method.getName());
                    saved.set((Cookie) args[0]);
                    return null;
                });
        SessionSecurity.setTokenCookie(request, response, "session-a");
        Cookie cookie = saved.get();
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
        assertEquals("Lax", cookie.getAttribute("SameSite"));
        assertEquals("/", cookie.getPath());
        assertNull(cookie.getDomain());
        assertEquals("session-a", SessionSecurity.readToken(request));
        SessionSecurity.clearTokenCookie(request, response);
        assertEquals(0, saved.get().getMaxAge());
    }
}
