package com.myblog.sso.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.utils.Result;
import com.myblog.common.security.SessionSecurity;
import jakarta.servlet.http.Cookie;
import com.myblog.sso.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TokenControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> lookedUpToken = new AtomicReference<>();
    private MockMvc mockMvc;
    private final AtomicReference<String> loggedOutToken = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        TokenController controller = new TokenController();
        TokenService tokenService = new TokenService() {
            @Override public Result getUserByToken(String token) {
                lookedUpToken.set(token);
                return Result.ok(Map.of("username", "student"));
            }
            @Override public Result logout(String token) {
                loggedOutToken.set(token);
                return Result.ok();
            }
        };
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void logoutRequiresPostAndTokenBoundCsrfBeforeDeletingTheCookie() throws Exception {
        Cookie cookie = new Cookie(SessionSecurity.TOKEN_COOKIE, "active-token");
        var rejected = mockMvc.perform(post("/user/logout").cookie(cookie)).andReturn().getResponse();
        assertEquals(403, rejected.getStatus());
        assertNull(loggedOutToken.get());
        var response = mockMvc.perform(post("/user/logout").cookie(cookie)
                        .header("X-CSRF-Token", SessionSecurity.csrfToken("active-token")))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("active-token", loggedOutToken.get());
        assertEquals(0, response.getCookie(SessionSecurity.TOKEN_COOKIE).getMaxAge());
        assertEquals(405, mockMvc.perform(get("/user/logout")).andReturn().getResponse().getStatus());
    }

    @Test
    void returnsJsonUsingTheDefaultMvcMessageConverters() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/user/token/test-token"))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(response.getContentType())));
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(200, body.path("status").asInt());
        assertEquals("student", body.path("data").path("username").asText());
        assertEquals("test-token", lookedUpToken.get());
    }

    @Test
    void preservesJsonpForAValidCallback() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/user/token/test-token")
                        .param("callback", "app.onLogin"))
                .andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertEquals("application/javascript;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.startsWith("app.onLogin("));
        assertTrue(body.endsWith(");"));
        JsonNode result = objectMapper.readTree(body.substring("app.onLogin(".length(), body.length() - 2));
        assertEquals("student", result.path("data").path("username").asText());
        assertEquals("test-token", lookedUpToken.get());
    }

    @Test
    void rejectsAnInvalidCallbackBeforeLookingUpTheSession() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/user/token/test-token")
                        .param("callback", "alert(1)//"))
                .andReturn().getResponse();

        assertEquals(400, response.getStatus());
        assertEquals(400, objectMapper.readTree(response.getContentAsString()).path("status").asInt());
        assertNull(lookedUpToken.get());
    }
}
