package com.myblog.portal.service;

import com.myblog.portal.pojo.ArticleResult;
import com.myblog.pojo.TbArticle;

public interface ArticlePageService {
    ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy);
    TbArticle getArticleWithText(int articleId);
}
