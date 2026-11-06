package com.myblog.service;

import com.myblog.common.utils.Result;
import com.myblog.pojo.TbArticle;

import java.util.List;

public interface ArticleService {
    Result getArticleListPage(int pageNum, int pageSize, String orderBy);
    void deleteArticle(int articleId);
    void deleteBatch(List<Integer> articleIds);
    /** Saves an article, defaulting to DRAFT; retained for existing callers. */
    void addArticle(TbArticle tbArticle);
    /** Returns server-generated fields explicitly, including across a remote RPC boundary. */
    TbArticle createArticle(TbArticle tbArticle);
    TbArticle getArticle(int articleId);
    /** Patches editorial fields; omitted fields and the original publication time are retained. */
    void updateArticle(TbArticle tbArticle);
}
