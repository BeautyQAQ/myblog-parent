package com.myblog.service.content;

/** Compatibility facade for validation performed by the shared article HTML policy. */
public final class ArticleHtmlSanitizer {
    private ArticleHtmlSanitizer() {
    }

    public static String clean(String html) {
        return com.myblog.common.content.ArticleHtmlSanitizer.clean(html);
    }
}
