package com.myblog.portal.controller;

import com.myblog.pojo.TbArticle;
import com.myblog.pojo.TbUser;
import com.myblog.common.security.SessionSecurity;
import com.myblog.common.utils.Result;
import com.myblog.sso.service.TokenService;
import jakarta.servlet.http.Cookie;
import com.myblog.portal.pojo.ArticleResult;
import com.myblog.portal.service.ArticlePageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class PortalTemplatesTest {
    private GenericWebApplicationContext context;
    private MockMvc mvc;
    private final StubArticlePageService service = new StubArticlePageService();
    private final StubTokenService tokens = new StubTokenService();

    @BeforeEach
    void setUp() throws Exception {
        context = new GenericWebApplicationContext();
        context.setServletContext(new MockServletContext("src/main/webapp", new FileSystemResourceLoader()));
        context.registerBean("articlePageService", ArticlePageService.class, () -> service);
        context.registerBean("tokenService", TokenService.class, () -> tokens);
        new XmlBeanDefinitionReader(context).loadBeanDefinitions(mvcXmlWithoutDubbo());
        context.refresh();
        mvc = webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void rendersTheListAndKeepsContextPathSortAndPageSizeInPagination() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/blog/").contextPath("/blog")
                        .param("pageNum", "2").param("pageSize", "20").param("orderBy", "heat"))
                .andReturn().getResponse();
        String html = response.getContentAsString();

        assertEquals(200, response.getStatus());
        assertEquals("UTF-8", response.getCharacterEncoding());
        assertEquals("heat", service.orderBy);
        assertEquals(20, service.pageSize);
        assertTrue(html.contains("href=\"/blog/blogs?articleId=42\""));
        assertTrue(html.contains("href=\"/blog/?pageNum=3&amp;pageSize=20&amp;orderBy=heat\""));
        assertTrue(html.contains("href=\"/blog/?pageNum=1&amp;pageSize=20&amp;orderBy=heat\""));
        assertTrue(html.contains("href=\"/blog/css/bootstrap.min.css\""));
        assertTrue(html.contains("src=\"/blog/resources/images/avatar-defualt.jpg\""));
        assertTrue(html.contains("href=\"http://localhost:8086/page/login\""));
        assertTrue(html.contains("&lt;script&gt;标题&lt;/script&gt;"));
        assertTrue(html.contains("正文 加粗"));
        assertFalse(html.contains("<p>正文"));
        assertEquals(1, occurrences(html, "<html"));
        assertEquals(1, occurrences(html, "<head>"));
        assertEquals(1, occurrences(html, "<body"));
        assertFalse(html.contains("${"));
    }

    @Test
    void rendersRichArticleHtmlAndEscapesItsTitle() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/blog/blogs").contextPath("/blog")
                        .param("articleId", "42"))
                .andReturn().getResponse();
        String html = response.getContentAsString();

        assertEquals(200, response.getStatus());
        assertEquals(42, service.articleId);
        assertTrue(html.contains("<title>&lt;script&gt;标题&lt;/script&gt;</title>"));
        assertTrue(html.contains("<p>正文 <strong>加粗</strong></p>"));
        assertTrue(html.contains("标签："));
        assertTrue(html.contains("src=\"/blog/js/main.js\""));
        assertFalse(html.contains("blogModel"));
        assertFalse(html.contains("<script>标题</script>"));
        assertEquals(1, occurrences(html, "<html"));
        assertEquals(1, occurrences(html, "<body"));
    }

    @Test
    void rendersAnEmptyListWithoutBrokenPagination() throws Exception {
        service.empty = true;
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("暂无文章"));
        assertFalse(html.contains("aria-label=\"文章分页\""));
        assertFalse(html.contains("pageNum=0"));
    }

    @Test
    void returnsAFriendly404ForAnUnpublishedDeletedOrUnknownArticle() throws Exception {
        service.missing = true;
        MockHttpServletResponse response = mvc.perform(get("/blogs").param("articleId", "42"))
                .andReturn().getResponse();
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains("文章不存在"));
        assertTrue(response.getContentAsString().contains("返回首页"));
    }

    @Test
    void rejectsInvalidPaginationBeforeCallingTheRemoteService() throws Exception {
        for (String[] parameter : List.of(new String[]{"pageNum", "0"}, new String[]{"pageSize", "101"},
                new String[]{"pageSize", "0"}, new String[]{"pageNum", "oops"}, new String[]{"orderBy", "sql"})) {
            MockHttpServletResponse response = mvc.perform(get("/").param(parameter[0], parameter[1]))
                    .andReturn().getResponse();
            assertEquals(400, response.getStatus());
            assertTrue(response.getContentAsString().contains("请求参数有误"));
        }
        assertEquals(0, service.listQueries);
    }

    @Test
    void rejectsMissingOrInvalidArticleIdsWithAFriendly400() throws Exception {
        for (String id : List.of("", "0", "-1", "oops")) {
            MockHttpServletResponse response = mvc.perform(get("/blogs").param("articleId", id))
                    .andReturn().getResponse();
            assertEquals(400, response.getStatus());
            assertTrue(response.getContentAsString().contains("请求参数有误"));
        }
        assertEquals(0, service.articleId);
    }

    @Test
    void emitsPlainTextSummariesWithoutMutatingTheArticleBody() throws Exception {
        String body = "<p onclick=\"alert(1)\">正文 &lt;img src=x onerror=alert(1)&gt;</p><script>alert(2)</script>";
        service.selected.setArticleText(body);
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("正文 &lt;img src=x onerror=alert(1)&gt;"));
        assertFalse(html.contains("<p onclick"));
        assertFalse(html.contains("<script>alert(2)</script>"));
        assertEquals(body, service.selected.getArticleText());
    }

    @Test
    void sanitizesLegacyBodiesWhenRenderingAnArticle() throws Exception {
        service.selected.setArticleText("<p onclick='alert(1)' style='color: red'>正文<strong>加粗</strong></p>"
                + "<img src='/images/legacy' onerror='alert(2)'><a href='javascript:alert(3)'>链接</a>"
                + "<script>alert(4)</script><iframe src='https://example.com'></iframe>");
        String html = mvc.perform(get("/blogs").param("articleId", "42")).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("正文<strong>加粗</strong>"));
        assertTrue(html.contains("/images/legacy"));
        assertTrue(html.contains("color: red"));
        assertFalse(html.contains("onclick="));
        assertFalse(html.contains("onerror="));
        assertFalse(html.contains("javascript:"));
        assertFalse(html.contains("<script>alert(4)"));
        assertFalse(html.contains("<iframe"));
    }

    @Test
    void handlesNullBodiesAndUsesPublicationTimeInsteadOfEditTime() throws Exception {
        service.selected.setArticleText(null);
        service.selected.setPublishedAt(Date.from(Instant.parse("2026-02-01T10:00:00Z")));
        String html = mvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("发表于 2026-02-01"));
        assertFalse(html.contains("发表于 2026-03-01"));
        service.selected.setPublishedAt(null);
        html = mvc.perform(get("/blogs").param("articleId", "42")).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("发表于 2026-01-01"));
    }

    @Test
    void anonymousReadingSkipsTheAccountServiceAndToleratesItsOutageWithAStaleCookie() throws Exception {
        assertEquals(200, mvc.perform(get("/")).andReturn().getResponse().getStatus());
        assertEquals(0, tokens.reads);
        tokens.unavailable = true;
        MockHttpServletResponse response = mvc.perform(get("/")
                .cookie(new Cookie(SessionSecurity.TOKEN_COOKIE, "stale-token"))).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(1, tokens.reads);
        assertTrue(response.getContentAsString().contains("登录</a>"));
    }

    @Test
    void rendersTheCurrentUserAndOnlyShowsManagementToAdministrators() throws Exception {
        tokens.user = new TbUser();
        tokens.user.setUsername("<b>reader</b>");
        tokens.user.setRole("USER");
        Cookie cookie = new Cookie(SessionSecurity.TOKEN_COOKIE, "signed-in-token");
        String html = mvc.perform(get("/").cookie(cookie)).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("&lt;b&gt;reader&lt;/b&gt;"));
        assertTrue(html.contains("退出登录"));
        assertTrue(html.contains(SessionSecurity.csrfToken("signed-in-token")));
        assertFalse(html.contains("管理文章"));
        assertFalse(html.contains("登录</a>"));
        tokens.user.setRole("ADMIN");
        html = mvc.perform(get("/").cookie(cookie)).andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("href=\"http://localhost:8084/\""));
        assertTrue(html.contains("管理文章"));
    }

    @Test
    void logoutRequiresCsrfAndClearsTheSharedCookieOnlyAfterTheSessionIsRevoked() throws Exception {
        Cookie cookie = new Cookie(SessionSecurity.TOKEN_COOKIE, "signed-in-token");
        assertEquals(403, mvc.perform(post("/auth/logout").cookie(cookie)).andReturn().getResponse().getStatus());
        assertEquals(0, tokens.logouts);
        MockHttpServletResponse response = mvc.perform(post("/auth/logout").cookie(cookie)
                .header("X-CSRF-Token", SessionSecurity.csrfToken("signed-in-token"))).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(1, tokens.logouts);
        assertNotNull(response.getCookie(SessionSecurity.TOKEN_COOKIE));
        assertEquals(0, response.getCookie(SessionSecurity.TOKEN_COOKIE).getMaxAge());
        assertEquals("/", response.getCookie(SessionSecurity.TOKEN_COOKIE).getPath());
    }

    @Test
    void aFailedLogoutKeepsTheCookieAndGivesARetryableError() throws Exception {
        tokens.unavailable = true;
        MockHttpServletResponse response = mvc.perform(post("/auth/logout")
                .cookie(new Cookie(SessionSecurity.TOKEN_COOKIE, "signed-in-token"))
                .header("X-CSRF-Token", SessionSecurity.csrfToken("signed-in-token"))).andReturn().getResponse();
        assertEquals(503, response.getStatus());
        assertNull(response.getCookie(SessionSecurity.TOKEN_COOKIE));
    }

    private static class StubTokenService implements TokenService {
        private TbUser user;
        private boolean unavailable;
        private int reads;
        private int logouts;
        @Override
        public Result getUserByToken(String token) {
            reads++;
            if (unavailable) throw new IllegalStateException("offline");
            return user == null ? Result.build(201, "expired") : Result.ok(user);
        }
        @Override
        public Result logout(String token) {
            logouts++;
            if (unavailable) throw new IllegalStateException("offline");
            return Result.ok();
        }
    }

    private static int occurrences(String value, String part) {
        return value.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    private static ByteArrayResource mvcXmlWithoutDubbo() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document;
        try (var stream = new ClassPathResource("spring/springmvc.xml").getInputStream()) {
            document = factory.newDocumentBuilder().parse(stream);
        }
        // Keep the production MVC and Thymeleaf configuration; only replace remote RPC with a stub.
        NodeList declarations = document.getElementsByTagNameNS("http://dubbo.apache.org/schema/dubbo", "*");
        while (declarations.getLength() > 0) {
            var node = declarations.item(0);
            node.getParentNode().removeChild(node);
        }
        TransformerFactory transformer = TransformerFactory.newInstance();
        transformer.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        transformer.newTransformer().transform(new DOMSource(document), new StreamResult(bytes));
        return new ByteArrayResource(bytes.toByteArray());
    }

    private static class StubArticlePageService implements ArticlePageService {
        private boolean empty;
        private boolean missing;
        private TbArticle selected = article();
        private int listQueries;
        private String orderBy;
        private int pageSize;
        private int articleId;

        @Override
        public ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy) {
            listQueries++;
            this.orderBy = orderBy;
            this.pageSize = pageSize;
            ArticleResult result = new ArticleResult();
            result.setArticleList(empty ? List.of() : List.of(selected));
            result.setPageNum(pageNum);
            result.setPages(empty ? 0 : 3);
            result.setHasPreviousPage(!empty && pageNum > 1);
            result.setHasNextPage(!empty && pageNum < 3);
            result.setNavigatepageNums(empty ? new int[0] : new int[]{1, 2, 3});
            return result;
        }

        @Override
        public TbArticle getArticleWithText(int articleId) {
            this.articleId = articleId;
            return missing ? null : selected;
        }

        private static TbArticle article() {
            TbArticle article = new TbArticle();
            article.setArticleId(42);
            article.setArticleTitle("<script>标题</script>");
            article.setArticleText("<p>正文 <strong>加粗</strong></p>");
            article.setArticleTag("Java");
            article.setArticlePageviews(12);
            article.setArticleLikecount(3);
            article.setArticleComment(2);
            article.setCreated(Date.from(Instant.parse("2026-01-01T10:00:00Z")));
            article.setUpdated(Date.from(Instant.parse("2026-03-01T10:00:00Z")));
            article.setArticleStatus("PUBLISHED");
            return article;
        }
    }
}
