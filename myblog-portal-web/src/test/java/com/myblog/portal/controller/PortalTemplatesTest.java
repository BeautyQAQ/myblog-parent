package com.myblog.portal.controller;

import com.myblog.pojo.TbArticle;
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
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class PortalTemplatesTest {
    private GenericWebApplicationContext context;
    private MockMvc mvc;
    private final StubArticlePageService service = new StubArticlePageService();

    @BeforeEach
    void setUp() throws Exception {
        context = new GenericWebApplicationContext();
        context.setServletContext(new MockServletContext("src/main/webapp", new FileSystemResourceLoader()));
        context.registerBean("articlePageService", ArticlePageService.class, () -> service);
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
        assertTrue(html.contains("<p>正文 加粗</p>"));
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
        private String orderBy;
        private int pageSize;
        private int articleId;

        @Override
        public ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy) {
            this.orderBy = orderBy;
            this.pageSize = pageSize;
            ArticleResult result = new ArticleResult();
            result.setArticleList(empty ? List.of() : List.of(article()));
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
            return article();
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
            article.setUpdated(Date.from(Instant.parse("2026-01-01T10:00:00Z")));
            return article;
        }
    }
}
