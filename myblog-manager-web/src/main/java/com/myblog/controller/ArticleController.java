package com.myblog.controller;

import com.myblog.common.utils.Result;
import com.myblog.pojo.TbArticle;
import com.myblog.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @RequestMapping("/")
    public String index(){
        return "index";
    }
    @RequestMapping("/article_admin")
    public String type(){
        return "article_admin";
    }

    @RequestMapping("/article/list")
    @ResponseBody
    public Result getArticleListPage(@RequestParam(value = "pageNum", defaultValue = "1")Integer pageNum,
                                 @RequestParam(value = "pageSize", defaultValue = "6")Integer pageSize,
                                 @RequestParam(value = "orderBy", defaultValue = "new")String orderBy){
        Result articleListPage = articleService.getArticleListPage(pageNum, pageSize, orderBy);
        return articleListPage;
    }


    /**
     * 删除
     * @param articleIds 文章id
     * @return json
     */
    @RequestMapping("/article/del/{articleIds}")
    @ResponseBody
    public Result deleteArticle(@PathVariable String articleIds){
        if (articleIds.contains("-")){
            //批量删除
            List<Integer> articleIdsList = new ArrayList<Integer>();
            String[] str_articleIds = articleIds.split("-");
            for (String str : str_articleIds) {
                articleIdsList.add(Integer.parseInt(str));
            }
            articleService.deleteBatch(articleIdsList);
        }else {
            //单个删除
            articleService.deleteArticle(Integer.parseInt(articleIds));
        }
        return Result.ok();
    }

    @RequestMapping("/article/add")
    @ResponseBody
    public Result addArticle(TbArticle tbArticle){
        if ("".equals(tbArticle.getArticleTitle())){
            return Result.build(400,"标题不能为空");
        }
        if ("<p><br></p>".equals(tbArticle.getArticleText())){
            return Result.build(400,"文章不能为空");
        }
        tbArticle.setArticleAuthorId(10000);
        tbArticle.setCreated(new Date());
        tbArticle.setUpdated(new Date());
        articleService.addArticle(tbArticle);
        return Result.ok();
    }

    @RequestMapping("/article/get/{articleId}")
    @ResponseBody
    public Result getArticle(@PathVariable Integer articleId){
        TbArticle tbArticle = articleService.getArticle(articleId);
        return Result.ok(tbArticle);
    }

    @RequestMapping("/article/update")
    @ResponseBody
    public Result updateArticle(TbArticle tbArticle){
        if ("".equals(tbArticle.getArticleTitle())){
            return Result.build(400,"标题不能为空");
        }
        System.out.println("++++++++++++++++++++++:"+tbArticle.getArticleText());
        if ("<p><br></p>".equals(tbArticle.getArticleText())){
            return Result.build(400,"文章不能为空");
        }
        tbArticle.getArticleId();
        tbArticle.setUpdated(new Date());
        articleService.updateArticle(tbArticle);
        return Result.ok();
    }
}
