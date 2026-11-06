package com.myblog.common.content;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Cleans editor HTML before it is persisted and rendered as article content. */
public final class ArticleHtmlSanitizer {
    private static final int MAX_TEXT_BYTES = 65_535;
    private static final Safelist ARTICLE_HTML = Safelist.relaxed()
            .addTags("s", "del", "hr")
            .addAttributes(":all", "style")
            .removeProtocols("a", "href", "ftp")
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer")
            .preserveRelativeLinks(true);

    private ArticleHtmlSanitizer() {
    }

    /** Validates and cleans new or edited article content before persisting it. */
    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("文章正文不能为空");
        }
        checkSize(html);
        String result = cleanForDisplay(html);
        Document document = Jsoup.parseBodyFragment(result);
        String text = document.body().text().replace('\u00a0', ' ').replace("\u200b", "").strip();
        boolean hasImage = document.select("img[src]").stream().anyMatch(image -> !image.attr("src").isBlank());
        if (text.isEmpty() && !hasImage) {
            throw new IllegalArgumentException("文章正文不能为空");
        }
        checkSize(result);
        return result;
    }

    /** Cleans legacy content for rendering; empty or long historical articles remain readable. */
    public static String cleanForDisplay(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // A fixed HTTPS base lets jsoup check relative URLs without changing their saved form.
        String cleaned = Jsoup.clean(html, "https://myblog.invalid/", ARTICLE_HTML,
                new Document.OutputSettings().prettyPrint(false).charset(StandardCharsets.UTF_8));
        Document document = Jsoup.parseBodyFragment(cleaned);
        document.outputSettings().prettyPrint(false).charset(StandardCharsets.UTF_8);
        for (Element element : document.select("[style]")) {
            String style = safeStyle(element.attr("style"));
            if (style.isEmpty()) {
                element.removeAttr("style");
            } else {
                element.attr("style", style);
            }
        }
        return document.body().html();
    }

    private static void checkSize(String html) {
        if (html.getBytes(StandardCharsets.UTF_8).length > MAX_TEXT_BYTES) {
            throw new IllegalArgumentException("文章正文不能超过 65535 字节");
        }
    }

    private static String safeStyle(String style) {
        Map<String, String> declarations = new LinkedHashMap<>();
        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon < 1) {
                continue;
            }
            String property = declaration.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).strip();
            boolean allowed = switch (property) {
                case "color", "background-color" -> value.matches("(?i)(#[0-9a-f]{3,8}|[a-z]{1,24}|rgba?\\([0-9.,%\\s]{1,40}\\))");
                case "font-size" -> value.matches("(?i)([0-9]{1,3}(\\.[0-9]{1,2})?(px|pt|em|rem|%)|small|medium|large|x-large|xx-large)");
                case "font-family" -> value.matches("[\\p{L}\\p{N}\\s,'\"_-]{1,100}");
                case "font-weight" -> value.matches("(?i)(normal|bold|bolder|lighter|[1-9]00)");
                case "font-style" -> value.matches("(?i)(normal|italic|oblique)");
                case "text-align" -> value.matches("(?i)(left|center|right|justify)");
                case "text-decoration" -> value.matches("(?i)(none|underline|line-through|underline line-through)");
                default -> false;
            };
            if (allowed) {
                declarations.put(property, value);
            }
        }
        return declarations.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((first, next) -> first + "; " + next).orElse("");
    }
}
