package com.myblog.portal.controller;

import com.myblog.pojo.TbArticle;
import com.myblog.common.content.ArticleHtmlSanitizer;
import com.myblog.portal.pojo.ArticleResult;
import com.myblog.portal.service.ArticlePageService;
import com.myblog.portal.service.ArticleInteractionService;
import com.myblog.portal.pojo.ArticleInteraction;
import com.myblog.pojo.TbUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.jsoup.Jsoup;
import org.springframework.beans.TypeMismatchException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class PortalArticleController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortalArticleController.class);
    private final ArticlePageService articlePageService;
    private final ArticleInteractionService interactionService;
    private final String portalPublicBaseUrl;

    public PortalArticleController(ArticlePageService articlePageService, ArticleInteractionService interactionService,
                                   @Value("${portal.public-base-url:http://localhost:8082}") String portalPublicBaseUrl) {
        this.articlePageService = articlePageService;
        this.interactionService = interactionService;
        this.portalPublicBaseUrl = portalPublicBaseUrl.replaceAll("/+$", "");
    }

    @ModelAttribute
    public void defaultSearchContext(Model model) {
        model.addAttribute("keyword", "");
        model.addAttribute("selectedTag", "");
        model.addAttribute("pageSize", 10);
        model.addAttribute("orderBy", "new");
    }

    @GetMapping("/blogs")
    public String getArticleWithText(@RequestParam("articleId") Integer articleId, Model model,
                                     HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        if (articleId < 1) {
            throw new IllegalArgumentException("articleId must be positive");
        }
        TbArticle article = articlePageService.getArticleWithText(articleId);
        if (article == null) {
            return missingArticle(model, response);
        }
        model.addAttribute("interactionAvailable", false);
        model.addAttribute("articleLiked", false);
        String destination = portalPublicBaseUrl + "/blogs?articleId=" + articleId;
        model.addAttribute("articleLoginUrl", model.getAttribute("ssoPublicBaseUrl") + "/page/login?redirect="
                + URLEncoder.encode(destination, StandardCharsets.UTF_8));
        // An uncertain account identity must not silently become an anonymous view or an unliked state.
        if (!Boolean.TRUE.equals(model.getAttribute("authenticationUnavailable"))) {
            TbUser user = model.getAttribute("currentUser") instanceof TbUser current ? current : null;
            Long userId = user == null ? null : user.getId();
            try {
                ArticleInteraction interaction = "HEAD".equals(request.getMethod())
                        ? interactionService.getInteraction(articleId, userId)
                        : interactionService.recordView(articleId,
                                PortalVisitorIdentity.key(request, response, userId), userId);
                if (interaction == null) return missingArticle(model, response);
                article.setArticlePageviews(interaction.getPageviews());
                article.setArticleLikecount(interaction.getLikeCount());
                article.setArticleHeat(interaction.getHeat());
                model.addAttribute("articleLiked", interaction.isLiked());
                model.addAttribute("interactionAvailable", true);
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to load article interaction for article {}", articleId);
            }
        }
        article.setArticleText(ArticleHtmlSanitizer.cleanForDisplay(article.getArticleText()));
        model.addAttribute("articleWithText", article);
        return "blog";
    }

    private static String missingArticle(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("errorTitle", "文章不存在");
        model.addAttribute("errorMessage", "这篇文章尚未发布、已下架或已被删除。");
        return "error";
    }

    @GetMapping("/")
    public String getArticleListPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                     @RequestParam(value = "orderBy", defaultValue = "new") String orderBy,
                                     @RequestParam(value = "q", defaultValue = "") String keyword,
                                     @RequestParam(value = "tag", defaultValue = "") String tag,
                                     Model model) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100
                || (!"new".equals(orderBy) && !"heat".equals(orderBy))) {
            throw new IllegalArgumentException("Invalid pagination or sort parameters");
        }
        String normalizedKeyword = normalizeFilter(keyword);
        String normalizedTag = normalizeFilter(tag);
        ArticleResult result = articlePageService.getPortalArticleListPage(pageNum, pageSize, orderBy,
                normalizedKeyword, normalizedTag);
        Map<Integer, String> summaries = new LinkedHashMap<>();
        for (TbArticle article : result.getArticleList()) {
            String text = Jsoup.parse(article.getArticleText() == null ? "" : article.getArticleText()).text();
            if (text.codePointCount(0, text.length()) > 100) {
                text = text.substring(0, text.offsetByCodePoints(0, 100)) + "…";
            }
            summaries.put(article.getArticleId(), text);
        }
        model.addAttribute("hasPreviousPage", result.isHasPreviousPage());
        model.addAttribute("hasNextPage", result.isHasNextPage());
        model.addAttribute("pages", result.getPages());
        model.addAttribute("pageNum", result.getPageNum());
        model.addAttribute("navigatepageNums", result.getNavigatepageNums());
        model.addAttribute("articleList", result.getArticleList());
        model.addAttribute("articleSummaries", summaries);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("orderBy", orderBy);
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("selectedTag", normalizedTag);
        model.addAttribute("hasFilters", !normalizedKeyword.isEmpty() || !normalizedTag.isEmpty());
        model.addAttribute("publishedTags", articlePageService.getPublishedTags());
        model.addAttribute("total", result.getTotal());
        return "index";
    }

    private static String normalizeFilter(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.codePointCount(0, normalized.length()) > 50) {
            throw new IllegalArgumentException("Search keywords and tags must not exceed 50 characters");
        }
        return normalized;
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class,
            TypeMismatchException.class})
    public String invalidRequest(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("errorTitle", "请求参数有误");
        model.addAttribute("errorMessage", "请检查文章链接或筛选参数。每页文章数应为 1 到 100，标题关键词和标签各不超过 50 个字符。");
        return "error";
    }
}
