package com.myblog.portal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.CommentPage;
import com.myblog.pojo.TbUser;
import com.myblog.service.CommentService;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PortalCommentControllerTest {
    private static final String TOKEN = "comment-reader-token";
    private static final String KEY = "62f34f77-8bd5-4af7-9c4a-59f8d71d1485";
    private MockMvc mvc;
    private final ArticleComment comment = new ArticleComment();
    private String lastMethod;
    private Object[] lastArguments;
    private int calls;
    private int authentications;
    private boolean missing;
    private RuntimeException failure;
    private RuntimeException authFailure;
    private TbUser account;

    @BeforeEach
    void setUp() {
        account = new TbUser(); account.setId(7L); account.setUsername("reader"); account.setRole("USER");
        comment.setId(4L); comment.setArticleId(42); comment.setUserId(7L); comment.setModeratedBy(9L);
        comment.setUsername("<img src=x onerror=alert(1)>"); comment.setContent("<script>alert(1)</script>\n正文");
        comment.setStatus("APPROVED"); comment.setCreated(new Date(100)); comment.setModeratedAt(new Date(200));
        CommentService service = (CommentService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{CommentService.class}, (proxy, method, args) -> {
                    calls++; lastMethod = method.getName(); lastArguments = args;
                    if (failure != null) throw failure;
                    if (missing) return null;
                    if (method.getReturnType() == ArticleComment.class) return comment;
                    CommentPage page = new CommentPage(); page.setComments(List.of(comment)); page.setTotal(23);
                    page.setPageNum(2); page.setPageSize(10); page.setPages(3); return page;
                });
        TokenService tokenService = new TokenService() {
            @Override public Result getUserByToken(String token) {
                authentications++;
                if (authFailure != null) throw authFailure;
                return account == null ? Result.build(401, "expired") : Result.ok(account);
            }
            @Override public Result logout(String token) { throw new AssertionError(); }
        };
        // Including the real advice verifies API requests do not resolve identity twice.
        mvc = standaloneSetup(new PortalCommentController(service, tokenService))
                .setControllerAdvice(new PortalPageModel(tokenService, "http://localhost:8086", "http://localhost:8084"))
                .build();
    }

    @Test
    void publicListingUsesOnlyTheApprovedQueryAndDoesNotExposeModerationIdentity() throws Exception {
        var response = mvc.perform(get("/articles/42/comments").param("pageNum", "2").param("pageSize", "10")
                .param("status", "PENDING").param("userId", "999")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("getPublishedComments", lastMethod);
        assertArrayEquals(new Object[]{42, 2, 10}, lastArguments);
        JsonNode data = json(response).get("data");
        assertEquals(23, data.get("total").asInt());
        assertEquals(3, data.get("pages").asInt());
        JsonNode row = data.get("comments").get(0);
        assertEquals(comment.getContent(), row.get("content").asText());
        assertFalse(row.has("userId")); assertFalse(row.has("moderatedBy")); assertFalse(row.has("status"));
        assertEquals(0, authentications);
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void ownListUsesOnlyTheAuthenticatedAccountAndIncludesReviewState() throws Exception {
        comment.setStatus("REJECTED");
        var response = mvc.perform(member(get("/articles/42/comments/mine").param("userId", "999")))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("getOwnComments", lastMethod);
        assertArrayEquals(new Object[]{42, 7L, 1, 10}, lastArguments);
        assertEquals("REJECTED", json(response).at("/data/comments/0/status").asText());
        assertEquals(1, authentications);
    }

    @Test
    void submissionIgnoresAuthorAndApprovalInjectionAndPreservesIdempotencyKey() throws Exception {
        comment.setStatus("PENDING");
        var response = mvc.perform(write(post("/articles/42/comments").param("content", "text")
                .param("requestId", KEY).param("userId", "999").param("status", "APPROVED")))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("submitComment", lastMethod);
        assertArrayEquals(new Object[]{42, 7L, "text", KEY}, lastArguments);
        assertEquals("PENDING", json(response).at("/data/status").asText());
        assertEquals(1, authentications);
    }

    @Test
    void ownDeletionUsesTheVerifiedAccountAndDeletedRetryStatusIsPreserved() throws Exception {
        comment.setStatus("DELETED");
        var response = mvc.perform(write(post("/comments/4/delete").param("userId", "999")))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertArrayEquals(new Object[]{4L, 7L}, lastArguments);
        assertEquals("deleteOwnComment", lastMethod);
        assertEquals("DELETED", json(response).at("/data/status").asText());
        assertEquals(1, authentications);
    }

    @Test
    void anonymousAndCsrfFailuresNeverReachTheBackend() throws Exception {
        assertEquals(401, mvc.perform(get("/articles/42/comments/mine")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/articles/42/comments").param("content", "text").param("requestId", KEY))
                .andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/comments/4/delete")).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(member(post("/articles/42/comments").param("content", "text").param("requestId", KEY)))
                .andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(member(post("/comments/4/delete")).header("X-CSRF-Token", "fake"))
                .andReturn().getResponse().getStatus());
        assertEquals(0, authentications); assertEquals(0, calls);
    }

    @Test
    void expiredLoginAndAccountServiceFailureCannotReadPrivateCommentsOrWrite() throws Exception {
        account = null;
        assertEquals(401, mvc.perform(member(get("/articles/42/comments/mine"))).andReturn().getResponse().getStatus());
        authFailure = new IllegalStateException("SSO down");
        assertEquals(503, mvc.perform(write(post("/comments/4/delete"))).andReturn().getResponse().getStatus());
        assertEquals(0, calls);
    }

    @Test
    void hiddenArticlesAndOtherAuthorsCommentsBecomeNotFound() throws Exception {
        missing = true;
        assertEquals(404, mvc.perform(get("/articles/42/comments")).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(member(get("/articles/42/comments/mine"))).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(write(post("/comments/4/delete"))).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(write(post("/articles/42/comments").param("content", "text").param("requestId", KEY)))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void invalidPaginationAndMissingPayloadReturnFriendly400() throws Exception {
        for (String path : List.of("/articles/0/comments", "/articles/42/comments?pageNum=0",
                "/articles/42/comments?pageSize=51", "/articles/42/comments?pageSize=0", "/articles/no/comments")) {
            assertEquals(400, mvc.perform(get(path)).andReturn().getResponse().getStatus(), path);
        }
        assertEquals(400, mvc.perform(write(post("/articles/42/comments").param("content", "text")))
                .andReturn().getResponse().getStatus());
        assertEquals(0, calls);
    }

    @Test
    void backendValidationAndOutagesProduceRetryableJsonErrors() throws Exception {
        failure = new IllegalArgumentException("评论最多1000字");
        var invalid = mvc.perform(write(post("/articles/42/comments").param("content", "text").param("requestId", KEY)))
                .andReturn().getResponse();
        assertEquals(400, invalid.getStatus());
        assertTrue(json(invalid).get("msg").asText().contains("1000"));
        failure = new IllegalStateException("Database unavailable");
        var outage = mvc.perform(get("/articles/42/comments")).andReturn().getResponse();
        assertEquals(503, outage.getStatus());
        assertFalse(outage.getContentAsString().contains("Database unavailable"));
    }

    @Test
    void getRequestsCannotSubmitOrDeleteComments() throws Exception {
        assertEquals(405, mvc.perform(member(get("/comments/4/delete"))).andReturn().getResponse().getStatus());
        assertEquals(0, calls);
    }

    private static MockHttpServletRequestBuilder member(MockHttpServletRequestBuilder builder) {
        return builder.cookie(new Cookie(SessionSecurity.TOKEN_COOKIE, TOKEN));
    }
    private static MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder builder) {
        return member(builder).header("X-CSRF-Token", SessionSecurity.csrfToken(TOKEN));
    }
    private static JsonNode json(MockHttpServletResponse response) throws Exception {
        return new ObjectMapper().readTree(response.getContentAsString());
    }
}
