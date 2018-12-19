package com.myblog.portal.controller;

import com.myblog.portal.pojo.ArticleResult;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.service.ArticlePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PortalArticleController {

    @Autowired
    private ArticlePageService articlePageService;

    @RequestMapping("/blogs")
    public String getArticleWithText(@RequestParam("articleId") Integer articleId,Model model){
        TbArticle articleWithText = articlePageService.getArticleWithText(articleId);
        model.addAttribute("articleWithText",articleWithText);
        return "blog";
    }

    //@RequestMapping("/article/list")
    @RequestMapping("/")
    public String getArticleListPage(@RequestParam(value = "pageNum", defaultValue = "1")Integer pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "10")Integer pageSize,
                                            @RequestParam(value = "orderBy", defaultValue = "new")String orderBy,
                                            Model model){
        ArticleResult portalArticleListPage = articlePageService.getPortalArticleListPage(pageNum, pageSize, orderBy);
        List<TbArticle> articleList = portalArticleListPage.getArticleList();
        List<TbArticle> articleList1= new ArrayList<>();
        String articleText,articleTextReplaceAll,substring;
        for (TbArticle article : articleList) {
            articleText = article.getArticleText();
            articleTextReplaceAll = articleText.replaceAll("<(?!p|br|/p|br/)[^>]+>", "");
            if (articleTextReplaceAll.length()>100){
                articleTextReplaceAll = articleTextReplaceAll.substring(0, 100);
            }
            article.setArticleText(articleTextReplaceAll);
            articleList1.add(article);
        }
        model.addAttribute("hasPreviousPage",portalArticleListPage.isHasPreviousPage());
        model.addAttribute("hasNextPage",portalArticleListPage.isHasNextPage());
        model.addAttribute("pages",portalArticleListPage.getPages());
        model.addAttribute("pageNum",portalArticleListPage.getPageNum());
        model.addAttribute("navigatepageNums",portalArticleListPage.getNavigatepageNums());
        model.addAttribute("articleList",articleList1);
        return "index";
    }
}
