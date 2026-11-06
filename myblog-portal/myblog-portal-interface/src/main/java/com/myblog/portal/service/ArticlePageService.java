package com.myblog.portal.service;

import com.myblog.portal.pojo.ArticleResult;
import com.myblog.pojo.TbArticle;
import java.util.List;

public interface ArticlePageService {
    ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy);
    ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy, String keyword, String tag);
    List<String> getPublishedTags();
    TbArticle getArticleWithText(int articleId);
}
