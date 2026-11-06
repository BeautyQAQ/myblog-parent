package com.myblog.portal.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.myblog.common.jedis.JedisClient;
import com.myblog.common.messaging.ArticleCacheEvents;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.message.ArticleCacheInvalidationListener;
import com.myblog.portal.pojo.ArticleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** Behavioral regressions for public search, isolated from the original pagination tests. */
class ArticleSearchServiceTest {
    private static final String PREFIX = "test:article-search";
    private final FakeRedis redis = new FakeRedis();
    private final List<SearchQuery> pageQueries = new ArrayList<>();
    private int tagQueries;
    private boolean failPageQuery;
    private boolean failTagQuery;
    private List<String> databaseTags = List.of("Java", "Spring");
    private final TbArticleMapper mapper = (TbArticleMapper) Proxy.newProxyInstance(
            TbArticleMapper.class.getClassLoader(), new Class<?>[]{TbArticleMapper.class}, (proxy, method, args) -> {
                if ("selectPublishedTags".equals(method.getName())) {
                    assertNull(PageHelper.getLocalPage(), "Tag enumeration must not inherit list pagination");
                    tagQueries++;
                    if (failTagQuery) throw new IllegalStateException("MySQL tags unavailable");
                    return new ArrayList<>(databaseTags);
                }
                assertEquals("selectPublishedArticles", method.getName(), "Public lists must use the published search mapper");
                Page<TbArticle> page = PageHelper.getLocalPage();
                assertNotNull(page, "Pagination must start immediately before the public SELECT");
                pageQueries.add(new SearchQuery((String) args[0], (String) args[1], (String) args[2],
                        page.getPageNum(), page.getPageSize()));
                if (failPageQuery) throw new IllegalStateException("MySQL articles unavailable");
                page.setTotal(137);
                TbArticle article = new TbArticle();
                article.setArticleId(pageQueries.size());
                article.setArticleTitle("Public search result");
                article.setArticleStatus("PUBLISHED");
                page.add(article);
                return page;
            });
    private final ArticlePageServiceImpl service = new ArticlePageServiceImpl(mapper, redis.client, 300, PREFIX);

    @AfterEach
    void clearPagination() {
        PageHelper.clearPage();
    }

    @Test
    void escapesEqualsPercentAndUnderscoreAsLiteralTitleCharactersBeforeBuildingTheLikePattern() {
        service.getPortalArticleListPage(1, 10, "new", "  A=%_B  ", " Java ");

        assertEquals(new SearchQuery("%A===%=_B%", "Java", "new", 1, 10), pageQueries.getFirst());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void stripsUnicodeWhitespaceAndSharesTheCacheForEquivalentOrEmptyConditions() {
        ArticleResult first = service.getPortalArticleListPage(1, 10, "new", "\u2003Java\u2003", "\u2003Spring\u2003");
        ArticleResult equivalent = service.getPortalArticleListPage(1, 10, "new", "Java", "Spring");
        assertEquals(1, pageQueries.size());
        assertEquals(first.getArticleList().getFirst().getArticleId(), equivalent.getArticleList().getFirst().getArticleId());
        assertEquals(new SearchQuery("%Java%", "Spring", "new", 1, 10), pageQueries.getFirst());

        service.getPortalArticleListPage(1, 10, "new", null, null);
        service.getPortalArticleListPage(1, 10, "new", "", "");
        service.getPortalArticleListPage(1, 10, "new", "\u2003 ", " \u2003");
        service.getPortalArticleListPage(1, 10, "new");
        assertEquals(2, pageQueries.size());
        assertEquals(new SearchQuery(null, null, "new", 1, 10), pageQueries.getLast());
    }

    @Test
    void acceptsFiftyUnicodeCodePointsButRejectsLongerConditionsBeforeTouchingInfrastructure() {
        String fifty = "😀".repeat(50);
        service.getPortalArticleListPage(1, 10, "new", "\u2003" + fifty + "\u2003", fifty);
        assertEquals("%" + fifty + "%", pageQueries.getFirst().titlePattern());
        assertEquals(fifty, pageQueries.getFirst().tag());
        int reads = redis.reads;
        int writes = redis.writes;
        assertThrows(IllegalArgumentException.class,
                () -> service.getPortalArticleListPage(1, 10, "new", fifty + "x", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.getPortalArticleListPage(1, 10, "new", null, fifty + "x"));
        assertEquals(reads, redis.reads);
        assertEquals(writes, redis.writes);
        assertEquals(1, pageQueries.size());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void returnsMatchingTotalAndPaginationMetadataAfterDatabaseQueriesAndCacheHits() {
        ArticleResult fresh = service.getPortalArticleListPage(2, 10, "heat", "Java", "Backend");
        ArticleResult cached = service.getPortalArticleListPage(2, 10, "heat", "Java", "Backend");
        for (ArticleResult result : List.of(fresh, cached)) {
            assertEquals(137L, result.getTotal());
            assertEquals(2, result.getPageNum());
            assertEquals(14, result.getPages());
            assertTrue(result.isHasNextPage());
            assertTrue(result.isHasPreviousPage());
            assertEquals("PUBLISHED", result.getArticleList().getFirst().getArticleStatus());
            assertNotNull(result.getNavigatepageNums());
        }
        assertEquals(1, pageQueries.size());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void separatesKeywordTagPagePageSizeAndSortInTheCache() {
        service.getPortalArticleListPage(1, 10, "new", "Java", null);
        service.getPortalArticleListPage(1, 10, "new", null, "Java");
        service.getPortalArticleListPage(1, 10, "new", "Java", "Backend");
        service.getPortalArticleListPage(2, 10, "new", "Java", "Backend");
        service.getPortalArticleListPage(1, 20, "new", "Java", "Backend");
        service.getPortalArticleListPage(1, 10, "heat", "Java", "Backend");
        assertEquals(6, pageQueries.size());
        assertEquals(6, redis.values.size());
        assertEquals(1, service.getPortalArticleListPage(1, 10, "new", "Java", null).getArticleList().getFirst().getArticleId());
        assertEquals(2, service.getPortalArticleListPage(1, 10, "new", null, "Java").getArticleList().getFirst().getArticleId());
        assertEquals(3, service.getPortalArticleListPage(1, 10, "new", "Java", "Backend").getArticleList().getFirst().getArticleId());
        assertEquals(6, pageQueries.size());
    }

    @Test
    void fieldBoundariesCannotCollideWhenSearchValuesContainCacheKeySeparators() {
        ArticleResult first = service.getPortalArticleListPage(1, 10, "new", "a:b", "c");
        ArticleResult second = service.getPortalArticleListPage(1, 10, "new", "a", "b:c");
        assertNotEquals(first.getArticleList().getFirst().getArticleId(), second.getArticleList().getFirst().getArticleId());
        assertEquals(2, pageQueries.size());
        service.getPortalArticleListPage(1, 10, "new", "a:b", "c");
        service.getPortalArticleListPage(1, 10, "new", "a", "b:c");
        assertEquals(2, pageQueries.size());
    }

    @Test
    void kafkaInvalidationRefreshesEverySearchConditionAndThePublishedTagList() {
        service.getPortalArticleListPage(1, 10, "new", "Java", null);
        service.getPortalArticleListPage(2, 10, "heat", "Java", "Backend");
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        databaseTags = List.of("Java", "Kafka");
        AtomicBoolean acknowledged = new AtomicBoolean();
        new ArticleCacheInvalidationListener(redis.client, PREFIX)
                .onMessage(ArticleCacheEvents.INVALIDATE, () -> acknowledged.set(true));

        service.getPortalArticleListPage(1, 10, "new", "Java", null);
        service.getPortalArticleListPage(2, 10, "heat", "Java", "Backend");
        assertEquals(List.of("Java", "Kafka"), service.getPublishedTags());
        assertTrue(acknowledged.get());
        assertEquals(4, pageQueries.size());
        assertEquals(2, tagQueries);
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void publishedTagsAreCachedSeparatelyAndDoNotStartPagination() {
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        assertEquals(1, tagQueries);
        assertTrue(pageQueries.isEmpty());
        assertNull(PageHelper.getLocalPage());
        service.getPortalArticleListPage(1, 10, "new", null, "Java");
        assertEquals(1, pageQueries.size());
        assertEquals(1, tagQueries);
    }

    @Test
    void redisReadFailureFallsBackToMySqlWithoutWritingIntoAnUnknownCacheNamespace() {
        redis.failReads = true;
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        assertEquals(137L, service.getPortalArticleListPage(1, 10, "new", "Java", null).getTotal());
        assertEquals(1, tagQueries);
        assertEquals(1, pageQueries.size());
        assertEquals(0, redis.writes);
        assertTrue(redis.values.isEmpty());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void redisWriteFailureCannotPreventSearchOrTagEnumeration() {
        redis.failWrites = true;
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        assertEquals(137L, service.getPortalArticleListPage(1, 10, "new", "Java", null).getTotal());
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        service.getPortalArticleListPage(1, 10, "new", "Java", null);
        assertEquals(2, tagQueries);
        assertEquals(2, pageQueries.size());
        assertTrue(redis.values.isEmpty());
        assertNull(PageHelper.getLocalPage());
    }

    @Test
    void failedDatabaseSearchAlwaysClearsPaginationAndDoesNotCacheAResult() {
        failPageQuery = true;
        assertThrows(IllegalStateException.class,
                () -> service.getPortalArticleListPage(1, 10, "new", "Java", "Backend"));
        assertNull(PageHelper.getLocalPage());
        assertTrue(redis.values.isEmpty());
        failPageQuery = false;
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        assertEquals(1, tagQueries);
    }

    @Test
    void failedTagEnumerationDoesNotPoisonTheTagCacheOrPagination() {
        failTagQuery = true;
        assertThrows(IllegalStateException.class, service::getPublishedTags);
        assertTrue(redis.values.isEmpty());
        assertNull(PageHelper.getLocalPage());
        failTagQuery = false;
        assertEquals(List.of("Java", "Spring"), service.getPublishedTags());
        assertEquals(2, tagQueries);
    }

    private record SearchQuery(String titlePattern, String tag, String orderBy, int page, int pageSize) {}

    private static final class FakeRedis {
        private final Map<String, String> values = new HashMap<>();
        private int reads;
        private int writes;
        private boolean failReads;
        private boolean failWrites;
        private final JedisClient client = (JedisClient) Proxy.newProxyInstance(JedisClient.class.getClassLoader(),
                new Class<?>[]{JedisClient.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "get" -> {
                        reads++;
                        if (failReads) throw new IllegalStateException("Redis read unavailable");
                        yield values.get((String) args[0]);
                    }
                    case "setex" -> {
                        writes++;
                        assertEquals(300, args[1]);
                        if (failWrites) throw new IllegalStateException("Redis write unavailable");
                        values.put((String) args[0], (String) args[2]);
                        yield "OK";
                    }
                    case "incr" -> {
                        String key = (String) args[0];
                        assertEquals(PREFIX + ":version", key);
                        long next = Long.parseLong(values.getOrDefault(key, "0")) + 1;
                        values.put(key, Long.toString(next));
                        yield next;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
