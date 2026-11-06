package com.myblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.controller.security.ManagerAccessInterceptor;
import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.CommentPage;
import com.myblog.pojo.TbUser;
import com.myblog.service.CommentService;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

class CommentAdministrationTest {
    private static final String TOKEN = "comment-admin";
    private final List<Object[]> calls = new ArrayList<>();
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private RuntimeException failure;
    private boolean missing;
    private String role = "ADMIN";
    private int identityReads;
    private final CommentService comments = (CommentService) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{CommentService.class}, (proxy, method, args) -> {
                calls.add(args);
                if (failure != null) throw failure;
                if (missing) return null;
                if (method.getName().equals("moderateComment")) {
                    ArticleComment comment = comment();
                    comment.setStatus((String) args[2]);
                    comment.setModeratedBy((Long) args[1]);
                    return comment;
                }
                if (method.getName().equals("getModerationQueue")) {
                    CommentPage page = new CommentPage();
                    page.setPageNum((int) args[2]);
                    page.setPageSize((int) args[3]);
                    page.setPages(3);
                    page.setTotal(22);
                    page.setComments(new ArrayList<>(List.of(comment())));
                    return page;
                }
                throw new UnsupportedOperationException(method.getName());
            });

    @BeforeEach
    void setUp() {
        TokenService tokens = new TokenService() {
            public Result getUserByToken(String token) {
                identityReads++;
                if (!TOKEN.equals(token)) return Result.build(201, "expired");
                TbUser user = new TbUser();
                user.setId(7L);
                user.setUsername("管理员");
                user.setRole(role);
                return Result.ok(user);
            }
            public Result logout(String token) { return Result.ok(); }
        };
        mvc = standaloneSetup(new CommentController(comments))
                .setControllerAdvice(new ManagerErrors(), new ManagerPageModel())
                .addInterceptors(new ManagerAccessInterceptor(tokens, "http://localhost:8086", "http://localhost:8084"))
                .build();
    }

    @Test
    void anonymousPageRedirectsToItsOwnLoginDestinationWhileApisRejectAccess() throws Exception {
        var page = mvc.perform(get("/comment_admin")).andReturn().getResponse();
        assertEquals(302, page.getStatus());
        assertTrue(page.getRedirectedUrl().endsWith("http%3A%2F%2Flocalhost%3A8084%2Fcomment_admin"));
        assertEquals(401, mvc.perform(get("/comment/list")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/comment/1/status").param("status", "APPROVED"))
                .andReturn().getResponse().getStatus());
        assertTrue(calls.isEmpty());
    }

    @Test
    void ordinaryUsersAndRevokedAdministratorsCannotReadOrModerate() throws Exception {
        role = "USER";
        assertEquals(403, mvc.perform(get("/comment_admin").cookie(cookie())).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(get("/comment/list").cookie(cookie())).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(post("/comment/1/status").cookie(cookie()).param("status", "APPROVED")
                .header("X-CSRF-Token", csrf())).andReturn().getResponse().getStatus());
        assertTrue(calls.isEmpty());
    }

    @Test
    void onlyPostWithMatchingCsrfCanModerate() throws Exception {
        assertEquals(403, mvc.perform(post("/comment/1/status").cookie(cookie()).param("status", "APPROVED"))
                .andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(post("/comment/1/status").cookie(cookie()).param("status", "APPROVED")
                .header("X-CSRF-Token", SessionSecurity.csrfToken("other"))).andReturn().getResponse().getStatus());
        assertEquals(405, mvc.perform(get("/comment/1/status").cookie(cookie()).param("status", "APPROVED"))
                .andReturn().getResponse().getStatus());
        assertTrue(calls.isEmpty());
    }

    @Test
    void reviewerComesFromVerifiedSessionAndOnlyOneIdentityLookupOccurs() throws Exception {
        var response = mvc.perform(post("/comment/13/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                .param("status", "APPROVED").param("adminId", "999").param("userId", "888")
                .param("moderatedBy", "666")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertArrayEquals(new Object[]{13L, 7L, "APPROVED"}, calls.getFirst());
        assertEquals(7L, json.readTree(response.getContentAsString()).path("data").path("moderatedBy").asLong());
        assertEquals(1, identityReads);
    }

    @Test
    void forwardsValidatedFiltersAndPreservesPlainTextInJson() throws Exception {
        var response = mvc.perform(get("/comment/list").cookie(cookie()).param("status", "REJECTED")
                .param("articleId", "8").param("pageNum", "2").param("pageSize", "10"))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertArrayEquals(new Object[]{"REJECTED", 8, 2, 10}, calls.getFirst());
        var data = json.readTree(response.getContentAsString(StandardCharsets.UTF_8)).path("data");
        assertEquals(2, data.path("pageNum").asInt());
        assertEquals(22, data.path("total").asLong());
        assertEquals("<img src=x onerror=alert(1)>\n下一行", data.path("comments").get(0).path("content").asText());
        assertTrue(response.getContentType().contains("application/json"));
    }

    @Test
    void invalidFiltersAndActionsNeverReachTheService() throws Exception {
        for (String[] invalid : new String[][]{{"status", "PUBLISHED"}, {"articleId", "0"}, {"pageNum", "0"},
                {"pageSize", "51"}, {"pageNum", "bad"}}) {
            assertEquals(400, mvc.perform(get("/comment/list").cookie(cookie()).param(invalid[0], invalid[1]))
                    .andReturn().getResponse().getStatus());
        }
        for (String status : new String[]{"PENDING", "ALL", "unknown"}) {
            assertEquals(400, mvc.perform(post("/comment/1/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                    .param("status", status)).andReturn().getResponse().getStatus());
        }
        assertEquals(400, mvc.perform(post("/comment/0/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                .param("status", "DELETED")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(post("/comment/1/status").cookie(cookie()).header("X-CSRF-Token", csrf()))
                .andReturn().getResponse().getStatus());
        assertTrue(calls.isEmpty());
    }

    @Test
    void missingCommentsReturn404AndServiceFailuresExplainUncertainCommit() throws Exception {
        missing = true;
        assertEquals(404, mvc.perform(post("/comment/1/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                .param("status", "DELETED")).andReturn().getResponse().getStatus());
        failure = new IllegalStateException("cache event failed after commit");
        var response = mvc.perform(post("/comment/1/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                .param("status", "APPROVED")).andReturn().getResponse();
        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("审核结果可能已保存"));
        failure = new SecurityException("role revoked");
        assertEquals(403, mvc.perform(post("/comment/1/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                .param("status", "APPROVED")).andReturn().getResponse().getStatus());
        failure = new IllegalArgumentException("已删除的评论不能恢复");
        assertEquals(400, mvc.perform(post("/comment/1/status").cookie(cookie()).header("X-CSRF-Token", csrf())
                .param("status", "APPROVED")).andReturn().getResponse().getStatus());
    }

    @Test
    void actualMvcRendersFilterPaginationAndSafeContextRelativeEndpoints() throws Exception {
        try (var context = MvcConfigurationTest.mvcContext()) {
            var response = webAppContextSetup(context).build().perform(get("/manager/comment_admin")
                    .contextPath("/manager").cookie(new Cookie(SessionSecurity.TOKEN_COOKIE, "admin-session"))
                    .param("status", "REJECTED").param("articleId", "8").param("pageNum", "2"))
                    .andReturn().getResponse();
            String html = response.getContentAsString(StandardCharsets.UTF_8);
            assertEquals(200, response.getStatus());
            assertTrue(html.contains("<title>评论审核</title>"));
            assertTrue(html.contains("data-list-url=\"/manager/comment/list\""));
            assertTrue(html.contains("data-action-url=\"/manager/comment/\""));
            assertTrue(html.contains("data-page=\"2\""));
            assertTrue(html.contains("value=\"REJECTED\" selected=\"selected\""));
            assertTrue(html.contains("value=\"8\""));
            assertTrue(html.contains("href=\"/manager/article_admin\""));
            assertTrue(html.contains("src=\"/manager/js/comment-admin.js\""));
            assertTrue(html.contains("aria-live=\"polite\""));
            assertFalse(html.contains("th:"));
        }
    }

    private static ArticleComment comment() {
        ArticleComment comment = new ArticleComment();
        comment.setId(13L);
        comment.setArticleId(8);
        comment.setUsername("<script>bad</script>");
        comment.setArticleTitle("<b>文章</b>");
        comment.setContent("<img src=x onerror=alert(1)>\n下一行");
        comment.setStatus("PENDING");
        return comment;
    }
    private static Cookie cookie() { return new Cookie(SessionSecurity.TOKEN_COOKIE, TOKEN); }
    private static String csrf() { return SessionSecurity.csrfToken(TOKEN); }
}
