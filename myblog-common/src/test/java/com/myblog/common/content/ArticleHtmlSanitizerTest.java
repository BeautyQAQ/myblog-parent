package com.myblog.common.content;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArticleHtmlSanitizerTest {
    @Test
    void readingLegacyContentRemovesActiveMarkupWithoutApplyingWritingLimits() {
        String legacy = "<p>" + "旧正文".repeat(8_000)
                + "</p><script>alert(1)</script><img src='/images/old' onerror='alert(1)'>"
                + "<a href='javascript:alert(1)'>链接</a>";
        String displayed = ArticleHtmlSanitizer.cleanForDisplay(legacy);
        assertFalse(displayed.contains("<script"));
        assertFalse(displayed.contains("onerror"));
        assertFalse(displayed.contains("javascript:"));
        assertEquals("/images/old", Jsoup.parseBodyFragment(displayed).selectFirst("img").attr("src"));
        assertTrue(displayed.contains("旧正文".repeat(8_000)));
        assertThrows(IllegalArgumentException.class, () -> ArticleHtmlSanitizer.clean(legacy));
    }

    @Test
    void emptyHistoricalArticlesRenderWithoutThrowing() {
        assertEquals("", ArticleHtmlSanitizer.cleanForDisplay(null));
        assertEquals("", ArticleHtmlSanitizer.cleanForDisplay(""));
        assertEquals("", ArticleHtmlSanitizer.cleanForDisplay("<script>alert(1)</script>"));
        assertThrows(IllegalArgumentException.class, () -> ArticleHtmlSanitizer.clean("<script>alert(1)</script>"));
    }
}
