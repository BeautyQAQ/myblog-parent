package com.myblog.service.impl;

import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.service.messaging.ArticleCacheInvalidationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class ArticleLifecycleTest {
    @Test
    void savesDraftsByDefaultAndDoesNotAcceptClientCountersIdsOrTimestamps() {
        AtomicInteger events = new AtomicInteger();
        TbArticle article = article();
        article.setArticleId(999);
        article.setCreated(new Date(1));
        article.setPublishedAt(new Date(1));
        article.setArticlePageviews(500);
        var service = service((name, args) -> {
            assertEquals("insertSelective", name);
            TbArticle saved = (TbArticle) args[0];
            assertEquals("DRAFT", saved.getArticleStatus());
            assertEquals("有效标题", saved.getArticleTitle());
            assertNull(saved.getArticleId());
            assertNull(saved.getPublishedAt());
            assertEquals(0, saved.getArticlePageviews());
            assertEquals(0, saved.getArticleLikecount());
            assertEquals(0, saved.getArticleComment());
            assertEquals(0, saved.getArticleHeat());
            assertEquals(42, saved.getArticleAuthorId());
            assertTrue(saved.getCreated().getTime() > 1);
            assertEquals(saved.getCreated(), saved.getUpdated());
            saved.setArticleId(7);
            return 1;
        }, events);
        service.addArticle(article);
        assertEquals(7, article.getArticleId());
        assertEquals(1, events.get());
    }

    @Test
    void publishingANewArticleSetsItsPublicationTime() {
        TbArticle article = article();
        article.setArticleStatus("PUBLISHED");
        var service = service((name, args) -> 1, new AtomicInteger());
        service.addArticle(article);
        assertEquals(article.getCreated(), article.getPublishedAt());
    }

    @Test
    void returnsServerGeneratedFieldsInTheCreationResultForRemoteCallers() {
        AtomicInteger events = new AtomicInteger();
        TbArticle submitted = article();
        submitted.setArticleStatus("PUBLISHED");
        var service = service((name, args) -> {
            assertEquals("insertSelective", name);
            ((TbArticle) args[0]).setArticleId(37);
            return 1;
        }, events);
        TbArticle saved = service.createArticle(submitted);
        assertEquals(37, saved.getArticleId());
        assertEquals("PUBLISHED", saved.getArticleStatus());
        assertNotNull(saved.getPublishedAt());
        assertEquals(saved.getCreated(), saved.getPublishedAt());
        assertEquals(1, events.get());
    }

    @Test
    void aStatusOnlyPatchPublishesAndOnlyWritesEditorialFields() {
        AtomicInteger events = new AtomicInteger();
        TbArticle existing = article();
        existing.setArticleId(7);
        existing.setArticleStatus("DRAFT");
        existing.setArticlePageviews(25);
        existing.setCreated(new Date(1));
        var service = service((name, args) -> {
            if (name.equals("selectByPrimaryKey")) {
                return existing;
            }
            assertEquals("updateByPrimaryKeySelective", name);
            TbArticle saved = (TbArticle) args[0];
            assertEquals("PUBLISHED", saved.getArticleStatus());
            assertEquals(existing.getArticleTitle(), saved.getArticleTitle());
            assertEquals(existing.getArticleText(), saved.getArticleText());
            assertNull(saved.getArticleAuthorId());
            assertNull(saved.getArticlePageviews());
            assertNull(saved.getArticleLikecount());
            assertNull(saved.getArticleComment());
            assertNull(saved.getArticleHeat());
            assertNull(saved.getCreated());
            assertNotNull(saved.getPublishedAt());
            assertNotNull(saved.getUpdated());
            return 1;
        }, events);
        TbArticle patch = new TbArticle();
        patch.setArticleId(7);
        patch.setArticleStatus("PUBLISHED");
        patch.setArticleAuthorId(999);
        patch.setArticlePageviews(999);
        patch.setCreated(new Date(999));
        patch.setPublishedAt(new Date(999));
        service.updateArticle(patch);
        assertEquals(1, events.get());
    }

    @Test
    void revisionsAndRepublishingRetainTheOriginalPublicationTime() {
        for (String previous : List.of("PUBLISHED", "OFFLINE")) {
            TbArticle existing = article();
            existing.setArticleId(7);
            existing.setArticleStatus(previous);
            existing.setPublishedAt(new Date(123));
            var service = service((name, args) -> {
                if (name.equals("selectByPrimaryKey")) {
                    return existing;
                }
                TbArticle changes = (TbArticle) args[0];
                // A null patch value leaves the stored publication timestamp untouched.
                assertNull(changes.getPublishedAt());
                assertEquals("PUBLISHED", changes.getArticleStatus());
                return 1;
            }, new AtomicInteger());
            TbArticle patch = new TbArticle();
            patch.setArticleId(7);
            patch.setArticleStatus("PUBLISHED");
            patch.setArticleText("<p>修改后的正文</p>");
            patch.setPublishedAt(new Date());
            service.updateArticle(patch);
            assertEquals(new Date(123), existing.getPublishedAt());
        }
    }

    @Test
    void omittingTheStatusOnAnUpdatePreservesTheExistingStatus() {
        TbArticle existing = article();
        existing.setArticleId(7);
        existing.setArticleStatus("OFFLINE");
        var service = service((name, args) -> {
            if (name.equals("selectByPrimaryKey")) {
                return existing;
            }
            assertEquals("OFFLINE", ((TbArticle) args[0]).getArticleStatus());
            return 1;
        }, new AtomicInteger());
        TbArticle patch = new TbArticle();
        patch.setArticleId(7);
        patch.setArticleTitle("修订标题");
        service.updateArticle(patch);
    }

    @Test
    void rejectsInvalidContentAndStatusesBeforeAnyInsertOrCacheEvent() {
        AtomicInteger events = new AtomicInteger();
        var service = service((name, args) -> fail("Must not write invalid content: " + name), events);
        for (String title : List.of("", "  ", "字".repeat(51))) {
            TbArticle article = article();
            article.setArticleTitle(title);
            assertThrows(IllegalArgumentException.class, () -> service.addArticle(article));
        }
        TbArticle longTag = article();
        longTag.setArticleTag("字".repeat(51));
        assertThrows(IllegalArgumentException.class, () -> service.addArticle(longTag));
        TbArticle invalidStatus = article();
        invalidStatus.setArticleStatus("SCHEDULED");
        assertThrows(IllegalArgumentException.class, () -> service.addArticle(invalidStatus));
        TbArticle emptyBody = article();
        emptyBody.setArticleText("<p><br>&nbsp;</p><script>alert(1)</script>");
        assertThrows(IllegalArgumentException.class, () -> service.addArticle(emptyBody));
        assertEquals(0, events.get());
    }

    @Test
    void missingUpdatesAndDeletesFailWithoutInvalidatingTheCache() {
        AtomicInteger events = new AtomicInteger();
        var service = service((name, args) -> name.equals("selectByPrimaryKey") ? null : 0, events);
        TbArticle patch = article();
        patch.setArticleId(7);
        assertEquals("文章不存在", assertThrows(IllegalArgumentException.class,
                () -> service.updateArticle(patch)).getMessage());
        assertEquals("文章不存在", assertThrows(IllegalArgumentException.class,
                () -> service.deleteArticle(7)).getMessage());
        assertEquals("文章不存在", assertThrows(IllegalArgumentException.class,
                () -> service.deleteBatch(List.of(7))).getMessage());
        assertEquals(0, events.get());
    }

    @Test
    void deletesExistingArticlesAndDeduplicatesBatchIds() {
        AtomicInteger events = new AtomicInteger();
        var service = service((name, args) -> switch (name) {
            case "deleteByPrimaryKey" -> 1;
            case "countByExample", "deleteByExample" -> 2;
            default -> fail("Unexpected method " + name);
        }, events);
        service.deleteArticle(7);
        service.deleteBatch(List.of(7, 8, 7));
        assertEquals(2, events.get());
        assertThrows(IllegalArgumentException.class, () -> service.deleteBatch(List.of()));
        assertThrows(IllegalArgumentException.class, () -> service.deleteBatch(List.of(-1)));
    }

    @Test
    void pageSizeIsCappedBeforeDatabaseAccess() {
        var service = service((name, args) -> fail("Must not query invalid pagination"), new AtomicInteger());
        assertThrows(IllegalArgumentException.class, () -> service.getArticleListPage(1, 101, "new"));
    }

    private static TbArticle article() {
        TbArticle article = new TbArticle();
        article.setArticleTitle("  有效标题  ");
        article.setArticleText("<p>正文</p>");
        article.setArticleTag("Java");
        article.setArticleAuthorId(42);
        return article;
    }

    private static ArticleServiceImpl service(BiFunction<String, Object[], Object> calls, AtomicInteger events) {
        TbArticleMapper mapper = (TbArticleMapper) Proxy.newProxyInstance(TbArticleMapper.class.getClassLoader(),
                new Class<?>[]{TbArticleMapper.class}, (proxy, method, args) -> calls.apply(method.getName(), args));
        ArticleServiceImpl service = new ArticleServiceImpl();
        ReflectionTestUtils.setField(service, "tbArticleMapper", mapper);
        ReflectionTestUtils.setField(service, "cacheInvalidationPublisher",
                new ArticleCacheInvalidationPublisher(null, "test") {
                    @Override
                    public void publishAfterCommit() {
                        events.incrementAndGet();
                    }
                });
        return service;
    }
}
