package com.myblog.portal.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.myblog.common.jedis.JedisClient;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArticleStatisticsCacheTest {
    private long now;
    private int leadingArticle = 1;
    private int likes;
    private int queries;
    private final Map<String, Entry> cache = new HashMap<>();
    private record Entry(String json, long expiresAt) {}

    private final JedisClient redis = (JedisClient) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{JedisClient.class}, (proxy, method, args) -> switch (method.getName()) {
                case "get" -> {
                    Entry entry = cache.get((String) args[0]);
                    yield entry != null && entry.expiresAt > now ? entry.json : null;
                }
                case "setex" -> {
                    cache.put((String) args[0], new Entry((String) args[2], now + (Integer) args[1]));
                    yield "OK";
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
    private final TbArticleMapper mapper = (TbArticleMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{TbArticleMapper.class}, (proxy, method, args) -> {
                assertEquals("selectPublishedArticles", method.getName());
                queries++;
                Page<TbArticle> page = PageHelper.getLocalPage();
                page.setTotal(1);
                TbArticle article = new TbArticle();
                article.setArticleId(leadingArticle);
                article.setArticleLikecount(likes);
                article.setArticleHeat(5 * likes);
                page.add(article);
                return page;
            });

    @Test
    void rankingAndDisplayedCountersRefreshWithinThirtySecondsWithoutGlobalInvalidation() {
        var service = new ArticlePageServiceImpl(mapper, redis, 300, "test:statistics");
        assertEquals(1, service.getPortalArticleListPage(1, 10, "heat").getArticleList().getFirst().getArticleId());
        leadingArticle = 2;
        likes = 7;
        now = 29;
        assertEquals(1, service.getPortalArticleListPage(1, 10, "heat").getArticleList().getFirst().getArticleId());
        now = 30;
        TbArticle refreshed = service.getPortalArticleListPage(1, 10, "heat").getArticleList().getFirst();
        assertEquals(2, refreshed.getArticleId());
        assertEquals(7, refreshed.getArticleLikecount());
        assertEquals(35, refreshed.getArticleHeat());
        assertEquals(2, queries);
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void respectsShorterConfiguredTtlAndSeparatesOldLongLivedPages() {
        var service = new ArticlePageServiceImpl(mapper, redis, 5, "test:statistics");
        service.getPortalArticleListPage(1, 10, "new");
        assertTrue(cache.keySet().stream().allMatch(key -> key.contains(":published:interactions:search:")));
        now = 5;
        service.getPortalArticleListPage(1, 10, "new");
        assertEquals(2, queries);
    }
}
