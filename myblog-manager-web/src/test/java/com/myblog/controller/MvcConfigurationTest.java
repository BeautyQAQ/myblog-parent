package com.myblog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.utils.Result;
import com.myblog.service.ArticleService;
import com.myblog.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class MvcConfigurationTest {
    @Test
    void loadsTheMvcXmlAndRendersJsonWithoutExternalServices() throws Exception {
        try (GenericWebApplicationContext context = mvcContext()) {
            assertInstanceOf(StandardServletMultipartResolver.class, context.getBean("multipartResolver"));
            assertInstanceOf(ThymeleafViewResolver.class, context.getBean("viewResolver"));
            MockMvc mockMvc = webAppContextSetup(context).build();
            MockHttpServletResponse response = mockMvc.perform(get("/article/list")).andReturn().getResponse();

            assertEquals(200, response.getStatus());
            assertEquals(200, new ObjectMapper().readTree(response.getContentAsString()).path("status").asInt());
        }
    }

    @Test
    void rendersTheManagementShellWithContextRelativeResourcesAndInitialIframe() throws Exception {
        try (GenericWebApplicationContext context = mvcContext()) {
            MockHttpServletResponse response = webAppContextSetup(context).build()
                    .perform(get("/manager/").contextPath("/manager")).andReturn().getResponse();

            String html = response.getContentAsString(StandardCharsets.UTF_8);
            assertEquals(200, response.getStatus());
            assertEquals("UTF-8", response.getCharacterEncoding());
            assertTrue(html.contains("<title>文章管理</title>"));
            assertTrue(html.contains("href=\"/manager/css/Site.css\""));
            assertTrue(html.contains("src=\"/manager/js/_layout.js\""));
            assertTrue(html.contains("<iframe src=\"/manager/article_admin\""));
            String avatarRule = html.lines().filter(line -> line.contains("background-image")).findFirst().orElse("");
            assertTrue(avatarRule.replace("\\", "").contains("/manager/resources/images/logo.png"), avatarRule);
            assertFalse(html.matches("(?s).*\\bth:[\\w-]+\\s*=.*"));
            assertNull(response.getForwardedUrl());
        }
    }

    @Test
    void rendersContextRelativeArticleAndUploadEndpointsForTheEditor() throws Exception {
        try (GenericWebApplicationContext context = mvcContext()) {
            MockHttpServletResponse response = webAppContextSetup(context).build()
                    .perform(get("/manager/article_admin").contextPath("/manager")).andReturn().getResponse();

            String html = response.getContentAsString(StandardCharsets.UTF_8);
            assertEquals(200, response.getStatus());
            assertTrue(html.contains("data-list-url=\"/manager/article/list\""));
            assertTrue(html.contains("data-delete-url=\"/manager/article/del/\""));
            assertTrue(html.contains("data-detail-url=\"/manager/article/get/\""));
            assertTrue(html.contains("data-add-url=\"/manager/article/add\""));
            assertTrue(html.contains("data-update-url=\"/manager/article/update\""));
            assertTrue(html.contains("data-upload-url=\"/manager/upload\""));
            assertTrue(html.contains("src=\"/manager/js/wangEditor.min.js\""));
            assertTrue(html.indexOf("src=\"/manager/js/article-admin.js\"")
                    < html.indexOf("src=\"/manager/js/editor.js\""));
            assertFalse(html.contains("${APP_PATH}"));
            assertFalse(html.matches("(?s).*\\bth:[\\w-]+\\s*=.*"));
            assertNull(response.getForwardedUrl());
        }
    }

    private static GenericWebApplicationContext mvcContext() throws Exception {
        GenericWebApplicationContext context = new GenericWebApplicationContext();
        try {
            context.setServletContext(new MockServletContext("src/main/webapp", new FileSystemResourceLoader()));
            ArticleService stub = (ArticleService) Proxy.newProxyInstance(ArticleService.class.getClassLoader(),
                    new Class<?>[]{ArticleService.class}, (proxy, method, args) -> {
                        if (method.getName().equals("getArticleListPage")) {
                            return Result.ok(List.of());
                        }
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "toString" -> "ArticleServiceStub";
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                default -> throw new UnsupportedOperationException(method.getName());
                            };
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            context.registerBean("articleService", ArticleService.class, () -> stub);
            context.registerBean("imageService", ImageService.class, () -> new ImageService() {
                @Override
                public String saveImage(byte[] data) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public com.myblog.pojo.TbImage getImage(String id) {
                    return null;
                }
            });
            new XmlBeanDefinitionReader(context).loadBeanDefinitions(mvcXmlWithoutDubbo());
            context.refresh();
            return context;
        } catch (Exception e) {
            context.close();
            throw e;
        }
    }

    @Test
    void configuresTheServlet61UploadLimits() throws Exception {
        Document descriptor = xmlFactory().newDocumentBuilder()
                .parse(Path.of("src/main/webapp/WEB-INF/web.xml").toFile());
        assertEquals("6.1", descriptor.getDocumentElement().getAttribute("version"));
        String namespace = "https://jakarta.ee/xml/ns/jakartaee";
        assertEquals("5242880", descriptor.getElementsByTagNameNS(namespace, "max-file-size").item(0).getTextContent());
        assertEquals("6291456", descriptor.getElementsByTagNameNS(namespace, "max-request-size").item(0).getTextContent());
    }

    private static ByteArrayResource mvcXmlWithoutDubbo() throws Exception {
        Document document;
        try (var stream = new ClassPathResource("spring/springmvc.xml").getInputStream()) {
            document = xmlFactory().newDocumentBuilder().parse(stream);
        }
        // Only RPC declarations are replaced; MVC, component scanning and properties use the production XML.
        NodeList dubboElements = document.getElementsByTagNameNS("http://dubbo.apache.org/schema/dubbo", "*");
        while (dubboElements.getLength() > 0) {
            var element = dubboElements.item(0);
            element.getParentNode().removeChild(element);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformerFactory.newTransformer().transform(new DOMSource(document), new StreamResult(output));
        return new ByteArrayResource(output.toByteArray());
    }

    private static DocumentBuilderFactory xmlFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }
}
