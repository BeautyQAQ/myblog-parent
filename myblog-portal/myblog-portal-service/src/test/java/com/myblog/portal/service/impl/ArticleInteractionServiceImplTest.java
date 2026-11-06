package com.myblog.portal.service.impl;

import com.myblog.mapper.ArticleInteractionMapper;
import com.myblog.pojo.TbArticle;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArticleInteractionServiceImplTest {
    private static final String VISITOR = "a".repeat(64);
    private final List<String> calls = new ArrayList<>();
    private final TestTransactions transactions = new TestTransactions();
    private boolean published = true;
    private boolean recentView;
    private boolean existingLike;
    private String failAt;
    private int saveViewResult = 1;
    private int views = 11;
    private int likes = 2;
    private final ArticleInteractionMapper mapper = (ArticleInteractionMapper) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{ArticleInteractionMapper.class}, (proxy, method, args) -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                calls.add(method.getName());
                if (method.getName().equals(failAt)) throw new IllegalStateException("Database write failed");
                return switch (method.getName()) {
                    case "selectPublishedForUpdate" -> {
                        assertEquals(12, args[0]);
                        if (!published) yield null;
                        TbArticle article = new TbArticle();
                        article.setArticleId(12);
                        article.setArticlePageviews(views);
                        article.setArticleLikecount(likes);
                        article.setArticleHeat(views + 5 * likes);
                        yield article;
                    }
                    case "hasRecentView" -> {
                        assertEquals(VISITOR, args[1]);
                        yield recentView;
                    }
                    case "saveView" -> {
                        recentView = true;
                        yield saveViewResult;
                    }
                    case "incrementViews" -> { views++; yield 1; }
                    case "hasLike" -> { assertEquals(26L, args[1]); yield existingLike; }
                    case "insertLike" -> { existingLike = true; yield 1; }
                    case "deleteLike" -> { existingLike = false; yield 1; }
                    case "changeLikeCount" -> { likes += (int) args[1]; yield 1; }
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
    private final ArticleInteractionServiceImpl service = new ArticleInteractionServiceImpl(mapper, transactions);

    @Test
    void invalidArgumentsNeverStartATransactionOrTouchStorage() {
        assertThrows(IllegalArgumentException.class, () -> service.recordView(0, VISITOR, null));
        assertThrows(IllegalArgumentException.class, () -> service.recordView(12, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.recordView(12, "visitor", null));
        assertThrows(IllegalArgumentException.class, () -> service.recordView(12, "A".repeat(64), null));
        assertThrows(IllegalArgumentException.class, () -> service.recordView(12, VISITOR, -1L));
        assertThrows(IllegalArgumentException.class, () -> service.getInteraction(0, null));
        assertThrows(IllegalArgumentException.class, () -> service.setLiked(12, 0, true));
        assertTrue(calls.isEmpty());
        assertEquals(0, transactions.begins);
    }

    @Test
    void hiddenAndMissingArticlesNeverReadOrChangeInteractionRecords() {
        published = false;
        assertNull(service.recordView(12, VISITOR, 26L));
        assertNull(service.getInteraction(12, 26L));
        assertNull(service.setLiked(12, 26L, true));
        assertEquals(List.of("selectPublishedForUpdate", "selectPublishedForUpdate", "selectPublishedForUpdate"), calls);
        assertEquals(3, transactions.commits);
    }

    @Test
    void firstAnonymousReadCountsOnceAndReturnsUpdatedCounters() {
        var result = service.recordView(12, VISITOR, null);
        assertEquals(12, result.getPageviews());
        assertEquals(2, result.getLikeCount());
        assertEquals(22, result.getHeat());
        assertFalse(result.isLiked());
        assertEquals(List.of("selectPublishedForUpdate", "hasRecentView", "saveView", "incrementViews",
                "selectPublishedForUpdate"), calls);
        assertEquals(1, transactions.commits);
    }

    @Test
    void refreshWithinTheWindowDoesNotExtendTheWindowOrIncrementCounters() {
        recentView = true;
        existingLike = true;
        var result = service.recordView(12, VISITOR, 26L);
        assertEquals(11, result.getPageviews());
        assertTrue(result.isLiked());
        assertEquals(List.of("selectPublishedForUpdate", "hasRecentView", "hasLike"), calls);
    }

    @Test
    void anExpiredWindowCountsRegardlessOfMysqlUpsertAffectedRowConvention() {
        for (int affected : new int[]{0, 1, 2}) {
            recentView = false;
            saveViewResult = affected;
            service.recordView(12, VISITOR, null);
        }
        assertEquals(14, views);
        assertEquals(3, transactions.commits);
    }

    @Test
    void settingAndClearingLikesIsIdempotentAndPreservesHistoricalAggregateCounts() {
        var first = service.setLiked(12, 26L, true);
        assertTrue(first.isLiked());
        assertEquals(3, first.getLikeCount());
        assertEquals(26, first.getHeat());
        calls.clear();
        var repeated = service.setLiked(12, 26L, true);
        assertEquals(3, repeated.getLikeCount());
        assertEquals(List.of("selectPublishedForUpdate", "hasLike"), calls);
        calls.clear();
        var cancelled = service.setLiked(12, 26L, false);
        assertFalse(cancelled.isLiked());
        assertEquals(2, cancelled.getLikeCount());
        assertEquals(21, cancelled.getHeat());
        calls.clear();
        service.setLiked(12, 26L, false);
        assertEquals(List.of("selectPublishedForUpdate", "hasLike"), calls);
        assertEquals(4, transactions.commits);
    }

    @Test
    void statusLookupReturnsLikeStateWithoutCountingAView() {
        existingLike = true;
        var result = service.getInteraction(12, 26L);
        assertTrue(result.isLiked());
        assertEquals(11, result.getPageviews());
        assertEquals(List.of("selectPublishedForUpdate", "hasLike"), calls);
    }

    @Test
    void failedViewIncrementRollsBackItsDeduplicationRecordSoRetryCanCount() {
        failAt = "incrementViews";
        assertThrows(IllegalStateException.class, () -> service.recordView(12, VISITOR, null));
        assertFalse(recentView);
        assertEquals(11, views);
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        failAt = null;
        assertEquals(12, service.recordView(12, VISITOR, null).getPageviews());
    }

    @Test
    void failedLikeCounterUpdateRollsBackTheLikeRecordSoRetryIsConsistent() {
        failAt = "changeLikeCount";
        assertThrows(IllegalStateException.class, () -> service.setLiked(12, 26L, true));
        assertFalse(existingLike);
        assertEquals(2, likes);
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
        failAt = null;
        assertEquals(3, service.setLiked(12, 26L, true).getLikeCount());
    }

    private class TestTransactions extends AbstractPlatformTransactionManager {
        int begins;
        int commits;
        int rollbacks;
        boolean oldRecentView;
        boolean oldLike;
        int oldViews;
        int oldLikes;

        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            begins++;
            oldRecentView = recentView;
            oldLike = existingLike;
            oldViews = views;
            oldLikes = likes;
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
            recentView = oldRecentView;
            existingLike = oldLike;
            views = oldViews;
            likes = oldLikes;
        }
    }
}
