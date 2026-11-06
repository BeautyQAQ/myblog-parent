package com.myblog.portal.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.myblog.common.jedis.JedisClient;
import com.myblog.common.messaging.ArticleCacheEvents;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.message.ArticleCacheInvalidationListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ArticlePageServiceImplTest {
    private final Map<String, String> cache = new HashMap<>();
    private final AtomicInteger queries = new AtomicInteger();
    private boolean failQuery;
    private final JedisClient redis = (JedisClient) Proxy.newProxyInstance(JedisClient.class.getClassLoader(),
            new Class<?>[]{JedisClient.class}, (proxy, method, args) -> switch (method.getName()) {
                case "get" -> cache.get((String) args[0]);
                case "setex" -> {
                    assertEquals(300, args[1]);
                    cache.put((String) args[0], (String) args[2]);
                    yield "OK";
                }
                case "incr" -> {
                    long version = Long.parseLong(cache.getOrDefault((String) args[0], "0")) + 1;
                    cache.put((String) args[0], Long.toString(version));
                    yield version;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            });
    private final TbArticleMapper mapper = (TbArticleMapper) Proxy.newProxyInstance(
            TbArticleMapper.class.getClassLoader(), new Class<?>[]{TbArticleMapper.class}, (proxy, method, args) -> {
                queries.incrementAndGet();
                assertNotNull(PageHelper.getLocalPage());
                if (failQuery) {
                    throw new IllegalStateException("MySQL unavailable");
                }
                Page<TbArticle> page = PageHelper.getLocalPage();
                page.setTotal(100);
                TbArticle article = new TbArticle();
                article.setArticleId(queries.get());
                page.add(article);
                return page;
            });
    private final ArticlePageServiceImpl service = new ArticlePageServiceImpl(mapper, redis, 300, "test:articles");

    @AfterEach
    void clearPagination() {
        PageHelper.clearPage();
    }

    @Test
    void cachesEachPageSizeAndOrderSeparately() {
        service.getPortalArticleListPage(1, 10, "new");
        service.getPortalArticleListPage(2, 10, "new");
        service.getPortalArticleListPage(1, 10, "heat");
        service.getPortalArticleListPage(1, 20, "new");

        assertEquals(4, queries.get());
        assertEquals(4, cache.size());
        assertTrue(cache.containsKey("test:articles:v0:new:1:10"));
        assertTrue(cache.containsKey("test:articles:v0:new:2:10"));
        assertTrue(cache.containsKey("test:articles:v0:heat:1:10"));
        assertTrue(cache.containsKey("test:articles:v0:new:1:20"));
    }

    @Test
    void aCacheHitDoesNotLeavePaginationOnTheThread() {
        service.getPortalArticleListPage(1, 10, "new");
        service.getPortalArticleListPage(1, 10, "new");

        assertEquals(1, queries.get());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void kafkaInvalidationMakesAllCachedPagesInaccessible() {
        service.getPortalArticleListPage(1, 10, "new");
        service.getPortalArticleListPage(2, 10, "heat");
        new ArticleCacheInvalidationListener(redis, "test:articles")
                .onMessage(ArticleCacheEvents.INVALIDATE, () -> {});

        service.getPortalArticleListPage(1, 10, "new");
        service.getPortalArticleListPage(2, 10, "heat");

        assertEquals(4, queries.get());
        assertTrue(cache.containsKey("test:articles:v1:new:1:10"));
        assertTrue(cache.containsKey("test:articles:v1:heat:2:10"));
    }

    @Test
    void aFailedQueryClearsThePaginationState() {
        failQuery = true;

        assertThrows(IllegalStateException.class, () -> service.getPortalArticleListPage(1, 10, "new"));

        assertNull(PageHelper.getLocalPage());
        assertTrue(cache.isEmpty());
    }
}
