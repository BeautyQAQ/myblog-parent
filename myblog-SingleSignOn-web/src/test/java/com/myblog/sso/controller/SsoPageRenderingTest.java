package com.myblog.sso.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.utils.Result;
import com.myblog.pojo.TbUser;
import com.myblog.sso.service.LoginService;
import com.myblog.sso.service.RegisterService;
import com.myblog.sso.service.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class SsoPageRenderingTest {
    private GenericWebApplicationContext context;
    private MockMvc mockMvc;
    private final AtomicReference<String> loginCredentials = new AtomicReference<>();
    private final AtomicReference<TbUser> registeredUser = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        context = new GenericWebApplicationContext();
        context.setServletContext(new MockServletContext("src/main/webapp", new FileSystemResourceLoader()));
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("page-test",
                Map.of("portal.public-base-url", "https://blog.example.test/blog/")));
        context.registerBean("loginService", LoginService.class, () -> (username, password) -> {
            loginCredentials.set(username + ":" + password);
            return Result.ok("test-session-token");
        });
        context.registerBean("registerService", RegisterService.class, () -> new RegisterService() {
            @Override
            public Result checkData(String param, int type) {
                return Result.ok(true);
            }

            @Override
            public Result register(TbUser user) {
                registeredUser.set(user);
                return Result.ok();
            }
        });
        context.registerBean("tokenService", TokenService.class, () -> token -> Result.ok());
        new XmlBeanDefinitionReader(context).loadBeanDefinitions(mvcXmlWithoutDubbo());
        context.refresh();
        mockMvc = webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void rendersLoginWithSharedFragmentsAndContextAwareLinks() throws Exception {
        assertInstanceOf(ThymeleafViewResolver.class, context.getBean("viewResolver"));
        MockHttpServletResponse response = mockMvc.perform(get("/sso/page/login").contextPath("/sso"))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals("text/html;charset=UTF-8", response.getContentType());
        String html = response.getContentAsString();
        assertTrue(html.contains("登录 · NewStarBlog"));
        assertTrue(html.contains("action=\"/sso/user/login\""));
        assertTrue(html.contains("href=\"/sso/page/register\""));
        assertTrue(html.contains("src=\"/sso/js/auth.js\""));
        assertTrue(html.contains("href=\"/sso/css/bootstrap.min.css\""));
        assertTrue(html.contains("href=\"https://blog.example.test/blog/\""));
        assertTrue(html.contains("data-success-url=\"https://blog.example.test/blog/\""));
        assertEquals(1, html.split("<html", -1).length - 1);
        assertEquals(1, html.split("<body", -1).length - 1);
        assertFalse(html.contains("th:"));
        assertFalse(html.contains("${"));
    }

    @Test
    void rendersRegistrationWithLocalSubmitAndLoginUrls() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/sso/page/register").contextPath("/sso"))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        String html = response.getContentAsString();
        assertTrue(html.contains("注册 · NewStarBlog"));
        assertTrue(html.contains("action=\"/sso/user/register\""));
        assertTrue(html.contains("data-success-url=\"/sso/page/login\""));
        assertTrue(html.contains("name=\"email\""));
        assertTrue(html.contains("name=\"phone\""));
        assertFalse(html.contains("localhost:"));
    }

    @Test
    void escapesThePortalReturnUrlAsAnHtmlAttribute() throws Exception {
        String html = mockMvc.perform(get("/page/login")
                        .param("redirect", "articles/42?tag=Java&orderBy=new"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(html.contains("data-success-url=\"https://blog.example.test/blog/articles/42?tag=Java&amp;orderBy=new\""));
        assertFalse(html.contains("<script type=\"text/javascript\">"));
    }

    @Test
    void rejectsScriptAndExternalReturnUrls() throws Exception {
        for (String redirect : new String[]{"javascript:alert(1)", "//evil.example/",
                "https://blog.example.test.evil.example/", "https://blog.example.test@evil.example/",
                "\"</script><script>alert(1)</script>"}) {
            String html = mockMvc.perform(get("/page/login").param("redirect", redirect))
                    .andReturn().getResponse().getContentAsString();
            assertTrue(html.contains("data-success-url=\"https://blog.example.test/blog/\""), redirect);
            assertFalse(html.contains(redirect), redirect);
        }
    }

    @Test
    void preservesLoginFormBindingAndTheSessionCookie() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/sso/user/login").contextPath("/sso")
                        .param("username", "student").param("password", "test-password"))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(200, new ObjectMapper().readTree(response.getContentAsString()).path("status").asInt());
        assertEquals("student:test-password", loginCredentials.get());
        assertNotNull(response.getCookie("MYBLOG_TOKEN"));
        assertEquals("test-session-token", response.getCookie("MYBLOG_TOKEN").getValue());
        assertEquals("/", response.getCookie("MYBLOG_TOKEN").getPath());
    }

    @Test
    void preservesRegistrationFormBinding() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/sso/user/register").contextPath("/sso")
                        .param("username", "student").param("password", "test-password")
                        .param("email", "student@example.test").param("phone", "12345678901"))
                .andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertEquals(200, new ObjectMapper().readTree(response.getContentAsString()).path("status").asInt());
        assertEquals("student", registeredUser.get().getUsername());
        assertEquals("student@example.test", registeredUser.get().getEmail());
        assertEquals("12345678901", registeredUser.get().getPhone());
    }

    private static ByteArrayResource mvcXmlWithoutDubbo() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document document;
        try (var input = new ClassPathResource("spring/springmvc.xml").getInputStream()) {
            document = factory.newDocumentBuilder().parse(input);
        }
        // Keep production MVC and template configuration; only replace RPC clients with in-process stubs.
        NodeList rpcElements = document.getElementsByTagNameNS("http://dubbo.apache.org/schema/dubbo", "*");
        while (rpcElements.getLength() > 0) {
            var element = rpcElements.item(0);
            element.getParentNode().removeChild(element);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformerFactory.newTransformer().transform(new DOMSource(document), new StreamResult(output));
        return new ByteArrayResource(output.toByteArray());
    }
}
