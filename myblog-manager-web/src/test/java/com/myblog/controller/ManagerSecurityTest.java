package com.myblog.controller;

import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.controller.security.ManagerAccessInterceptor;
import com.myblog.pojo.TbArticle;
import com.myblog.pojo.TbUser;
import com.myblog.service.ArticleService;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ManagerSecurityTest {
    private static final String TOKEN = "admin-session";
    private final Sessions sessions = new Sessions();
    private final Articles articles = new Articles();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ArticleController controller = new ArticleController();
        ReflectionTestUtils.setField(controller, "articleService", articles);
        mvc = standaloneSetup(controller, new ManagerLogoutController(sessions))
                .setControllerAdvice(new ManagerErrors(), new ManagerPageModel())
                .addInterceptors(new ManagerAccessInterceptor(sessions,
                        "http://localhost:8086", "http://localhost:8084"))
                .build();
    }

    @Test
    void rejectsAnonymousExpiredAndOrdinaryUsersBeforeAnyWrite() throws Exception {
        assertEquals(401, mvc.perform(post("/article/add")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/article/add").cookie(cookie("expired")))
                .andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(post("/article/add").cookie(cookie("reader"))
                .header("X-CSRF-Token", SessionSecurity.csrfToken("reader")))
                .andReturn().getResponse().getStatus());
        assertNull(articles.saved);
    }

    @Test
    void writeRequiresCsrfAndGetCannotDelete() throws Exception {
        assertEquals(403, mvc.perform(post("/article/add").cookie(cookie(TOKEN)))
                .andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(post("/article/add").cookie(cookie(TOKEN))
                .header("X-CSRF-Token", SessionSecurity.csrfToken("other-session")))
                .andReturn().getResponse().getStatus());
        assertEquals(405, mvc.perform(get("/article/del/1").cookie(cookie(TOKEN)))
                .andReturn().getResponse().getStatus());
        assertNull(articles.saved);
    }

    @Test
    void authorAndCountersCannotBeInjectedThroughTheArticleForm() throws Exception {
        var response = mvc.perform(post("/article/add").cookie(cookie(TOKEN))
                .header("X-CSRF-Token", SessionSecurity.csrfToken(TOKEN))
                .param("articleTitle", "正文测试").param("articleText", "<p>正文</p>")
                .param("articleStatus", "DRAFT").param("articleId", "999")
                .param("articleAuthorId", "888").param("articlePageviews", "99999"))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(7, articles.saved.getArticleAuthorId());
        assertEquals(1, articles.saved.getArticleId());
        assertNull(articles.saved.getArticlePageviews());
        assertEquals("DRAFT", articles.saved.getArticleStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void statusWritesAreAuthenticatedAndMissingArticleReturns404() throws Exception {
        assertEquals(200, mvc.perform(post("/article/status/1").cookie(cookie(TOKEN))
                .header("X-CSRF-Token", SessionSecurity.csrfToken(TOKEN))
                .param("articleStatus", "OFFLINE")).andReturn().getResponse().getStatus());
        assertEquals("OFFLINE", articles.saved.getArticleStatus());
        assertEquals(404, mvc.perform(get("/article/get/99").cookie(cookie(TOKEN)))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void logoutRevokesSessionAndClearsTheCookie() throws Exception {
        assertEquals(403, mvc.perform(post("/auth/logout").cookie(cookie(TOKEN)))
                .andReturn().getResponse().getStatus());
        assertTrue(sessions.active);
        var response = mvc.perform(post("/auth/logout").cookie(cookie(TOKEN))
                .header("X-CSRF-Token", SessionSecurity.csrfToken(TOKEN))).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(0, response.getCookie(SessionSecurity.TOKEN_COOKIE).getMaxAge());
        assertEquals(401, mvc.perform(get("/article/list").cookie(cookie(TOKEN)))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void unavailableSsoCannotGrantManagementAccess() throws Exception {
        sessions.unavailable = true;
        assertEquals(503, mvc.perform(get("/article/list").cookie(cookie(TOKEN)))
                .andReturn().getResponse().getStatus());
    }

    private static Cookie cookie(String value) { return new Cookie(SessionSecurity.TOKEN_COOKIE, value); }

    private static class Sessions implements TokenService {
        boolean active = true;
        boolean unavailable;
        public Result getUserByToken(String token) {
            if (unavailable) throw new IllegalStateException("offline");
            if (!active || !(TOKEN.equals(token) || "reader".equals(token))) return Result.build(201, "会话过期");
            TbUser user = new TbUser();
            user.setId(7L);
            user.setUsername("admin");
            user.setRole(TOKEN.equals(token) ? "ADMIN" : "USER");
            return Result.ok(user);
        }
        public Result logout(String token) { active = false; return Result.ok(); }
    }

    private static class Articles implements ArticleService {
        TbArticle saved;
        public Result getArticleListPage(int pageNum, int pageSize, String orderBy) { return Result.ok(List.of()); }
        public void deleteArticle(int id) { saved = null; }
        public void deleteBatch(List<Integer> ids) { saved = null; }
        public void addArticle(TbArticle article) { article.setArticleId(1); saved = article; }
        public TbArticle createArticle(TbArticle article) { addArticle(article); return saved; }
        public TbArticle getArticle(int id) { return id == 1 ? saved : null; }
        public void updateArticle(TbArticle article) { saved = article; }
    }
}
