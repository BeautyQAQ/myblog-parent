package com.myblog.controller;

import com.myblog.common.utils.Result;
import com.myblog.common.content.ArticleHtmlSanitizer;
import com.myblog.pojo.TbArticle;
import com.myblog.pojo.TbUser;
import com.myblog.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/article_admin")
    public String articleAdmin() { return "article_admin"; }

    @GetMapping("/article/list")
    @ResponseBody
    public Result getArticleListPage(@RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "6") int pageSize,
            @RequestParam(defaultValue = "new") String orderBy) {
        return articleService.getArticleListPage(pageNum, pageSize, orderBy);
    }

    @PostMapping("/article/del/{articleIds}")
    @ResponseBody
    public Result deleteArticle(@PathVariable String articleIds) {
        if (!articleIds.matches("[1-9][0-9]*(?:-[1-9][0-9]*){0,99}")) {
            throw new IllegalArgumentException("请选择有效的文章，每次最多删除100篇");
        }
        List<Integer> ids = Arrays.stream(articleIds.split("-")).map(Integer::valueOf).distinct().toList();
        if (ids.size() == 1) articleService.deleteArticle(ids.getFirst());
        else articleService.deleteBatch(ids);
        return Result.ok();
    }

    @PostMapping("/article/add")
    @ResponseBody
    public Result addArticle(@ModelAttribute ArticleForm form, @RequestAttribute TbUser currentUser) {
        TbArticle article = form.toArticle();
        article.setArticleId(null);
        article.setArticleAuthorId(Math.toIntExact(currentUser.getId()));
        return Result.ok(articleService.createArticle(article));
    }

    @GetMapping("/article/get/{articleId}")
    @ResponseBody
    public Result getArticle(@PathVariable int articleId) {
        TbArticle article = articleService.getArticle(articleId);
        if (article == null) throw new IllegalArgumentException("文章不存在");
        article.setArticleText(ArticleHtmlSanitizer.cleanForDisplay(article.getArticleText()));
        return Result.ok(article);
    }

    @PostMapping("/article/update")
    @ResponseBody
    public Result updateArticle(@ModelAttribute ArticleForm form) {
        TbArticle article = form.toArticle();
        articleService.updateArticle(article);
        return Result.ok(articleService.getArticle(article.getArticleId()));
    }

    @PostMapping("/article/status/{articleId}")
    @ResponseBody
    public Result changeStatus(@PathVariable int articleId, @RequestParam String articleStatus) {
        TbArticle patch = new TbArticle();
        patch.setArticleId(articleId);
        patch.setArticleStatus(articleStatus);
        articleService.updateArticle(patch);
        return Result.ok(articleService.getArticle(articleId));
    }

    /** Deliberately excludes author, counters, timestamps, and other server-owned fields. */
    public static class ArticleForm {
        private Integer articleId;
        private String articleTitle;
        private String articleTag;
        private String articleText;
        private String articleStatus;
        public Integer getArticleId() { return articleId; }
        public void setArticleId(Integer value) { articleId = value; }
        public String getArticleTitle() { return articleTitle; }
        public void setArticleTitle(String value) { articleTitle = value; }
        public String getArticleTag() { return articleTag; }
        public void setArticleTag(String value) { articleTag = value; }
        public String getArticleText() { return articleText; }
        public void setArticleText(String value) { articleText = value; }
        public String getArticleStatus() { return articleStatus; }
        public void setArticleStatus(String value) { articleStatus = value; }
        TbArticle toArticle() {
            TbArticle article = new TbArticle();
            article.setArticleId(articleId);
            article.setArticleTitle(articleTitle);
            article.setArticleTag(articleTag);
            article.setArticleText(articleText);
            article.setArticleStatus(articleStatus);
            return article;
        }
    }
}
