package com.myblog.service;

import com.myblog.common.utils.Result;
import com.myblog.pojo.TbArticle;

import java.util.List;

public interface ArticleService {
    Result getArticleListPage(int pageNum, int pageSize, String orderBy);
    void deleteArticle(int articleId);
    void deleteBatch(List<Integer> articleIds);
    void addArticle(TbArticle tbArticle);
    TbArticle getArticle(int articleId);
    void updateArticle(TbArticle tbArticle);
}
