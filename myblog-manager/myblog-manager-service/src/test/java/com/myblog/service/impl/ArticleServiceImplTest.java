package com.myblog.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArticleServiceImplTest {
    @Test
    void returnsTheExistingPaginationJsonUsingOnlyJdkCollections() {
        var service = new ArticleServiceImpl();
        TbArticleMapper mapper = (TbArticleMapper) Proxy.newProxyInstance(
                TbArticleMapper.class.getClassLoader(), new Class<?>[]{TbArticleMapper.class},
                (proxy, method, args) -> {
                    Page<TbArticle> page = PageHelper.getLocalPage();
                    page.setTotal(1);
                    TbArticle article = new TbArticle();
                    article.setArticleId(7);
                    article.setArticleTitle("RPC regression");
                    article.setCreated(new Date(123456789L));
                    page.add(article);
                    return page;
                });
        ReflectionTestUtils.setField(service, "tbArticleMapper", mapper);
        Object result = service.getArticleListPage(1, 10, "new").getData();
        Map<?, ?> page = assertInstanceOf(Map.class, result);
        assertEquals(1L, ((Number) page.get("total")).longValue());
        assertEquals(1, page.get("pageNum"));
        assertEquals(10, page.get("pageSize"));
        List<?> articles = assertInstanceOf(List.class, page.get("list"));
        Map<?, ?> article = assertInstanceOf(Map.class, articles.getFirst());
        assertEquals("RPC regression", article.get("articleTitle"));
        assertEquals(123456789L, ((Number) article.get("created")).longValue());
        assertFalse(articles instanceof Page<?>);
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void clearsPaginationWhenTheDatabaseQueryFails() {
        var service = new ArticleServiceImpl();
        TbArticleMapper mapper = (TbArticleMapper) Proxy.newProxyInstance(
                TbArticleMapper.class.getClassLoader(), new Class<?>[]{TbArticleMapper.class},
                (proxy, method, args) -> { throw new IllegalStateException("Database unavailable"); });
        ReflectionTestUtils.setField(service, "tbArticleMapper", mapper);
        assertThrows(IllegalStateException.class, () -> service.getArticleListPage(1, 10, "new"));
        assertNull(PageHelper.getLocalPage());
    }
}
