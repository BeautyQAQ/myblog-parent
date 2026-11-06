package com.myblog.sso.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.utils.Result;
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
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class TokenControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> lookedUpToken = new AtomicReference<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TokenController controller = new TokenController();
        TokenService tokenService = token -> {
            lookedUpToken.set(token);
            return Result.ok(Map.of("username", "student"));
        };
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        mockMvc = standaloneSetup(controller).build();
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
