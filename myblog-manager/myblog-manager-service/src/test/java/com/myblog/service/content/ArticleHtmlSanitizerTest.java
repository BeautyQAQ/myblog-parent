package com.myblog.service.content;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArticleHtmlSanitizerTest {
    @Test
    void stripsExecutableMarkupAndUrlsWhileRetainingEditorContentAndSafeStyles() {
        String html = ArticleHtmlSanitizer.clean("""
                <h2>标题</h2><p style="color: #e3370d; font-size: 24px; text-align: center; position: fixed; background-image: url(javascript:alert(1))">
                <strong>正文</strong><span style="font-family: 微软雅黑; font-style: italic">格式</span>
                <a href="javascript:alert(1)" onclick="alert(1)">危险链接</a>
                <a href="https://example.com/post">安全链接</a><a href="/blog/7">站内链接</a>
                <img src="/images/valid.png" onerror="alert(1)"><img src="data:image/svg+xml,evil">
                </p><script>alert(1)</script><iframe src="https://evil.example"></iframe>
                <pre><code>&lt;script&gt;literal code&lt;/script&gt;</code></pre>
                """);
        var document = Jsoup.parseBodyFragment(html);
        assertTrue(document.select("script, iframe, [onclick], [onerror]").isEmpty());
        assertFalse(html.contains("javascript:"));
        assertFalse(html.contains("data:"));
        assertFalse(html.contains("position:"));
        assertFalse(html.contains("background-image"));
        assertTrue(html.contains("color: #e3370d"));
        assertTrue(html.contains("font-size: 24px"));
        assertTrue(html.contains("text-align: center"));
        assertTrue(html.contains("font-family: 微软雅黑"));
        assertEquals("/images/valid.png", document.selectFirst("img[src]").attr("src"));
        assertEquals("https://example.com/post", document.select("a[href]").getFirst().attr("href"));
        assertEquals("/blog/7", document.select("a[href]").get(1).attr("href"));
        assertTrue(document.selectFirst("code").text().contains("<script>literal code</script>"));
        assertTrue(document.selectFirst("h2").text().contains("标题"));
    }

    @Test
    void rejectsBlankOrRemovedContentButAllowsAnImageOnlyArticle() {
        for (String html : new String[]{"", "  ", "<p><br></p>", "<div>&nbsp;\u200b</div>",
                "<script>alert(1)</script>", "<img src='javascript:alert(1)'>", "<img src=''>"}) {
            assertThrows(IllegalArgumentException.class, () -> ArticleHtmlSanitizer.clean(html), html);
        }
        assertThrows(IllegalArgumentException.class, () -> ArticleHtmlSanitizer.clean(null));
        assertTrue(ArticleHtmlSanitizer.clean("<img src='https://example.com/image.png'>").contains("image.png"));
    }

    @Test
    void enforcesTheMysqlTextLimitOnUtf8BytesBeforeAndAfterEscaping() {
        String maximum = "<p>" + "a".repeat(65_528) + "</p>";
        assertEquals(maximum, ArticleHtmlSanitizer.clean(maximum));
        assertThrows(IllegalArgumentException.class,
                () -> ArticleHtmlSanitizer.clean("<p>" + "a".repeat(65_529) + "</p>"));
        assertThrows(IllegalArgumentException.class,
                () -> ArticleHtmlSanitizer.clean("<p>" + "中".repeat(21_843) + "</p>"));
        // Raw input fits, but HTML escaping can make the stored representation exceed TEXT's byte limit.
        assertThrows(IllegalArgumentException.class,
                () -> ArticleHtmlSanitizer.clean("&".repeat(14_000)));
    }
}
