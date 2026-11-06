package com.myblog.service.impl;

import com.myblog.mapper.CommentMapper;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.TbArticle;
import com.myblog.pojo.TbUser;
import com.myblog.service.messaging.ArticleCacheInvalidationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommentServiceImplTest {
    private static final String REQUEST = "12345678-abcd-abcd-abcd-123456789012";
    private final List<String> calls = new ArrayList<>();
    private String articleStatus = "PUBLISHED";
    private ArticleComment stored;
    private String storedRequest;
    private int approved;
    private int events;
    private int commits;
    private int rollbacks;
    private boolean failCount;
    private boolean failEvent;
    private boolean admin = true;
    private String staleInitialStatus;
    private Object[] lastPage;
    private Object[] lastCount;
    private final CommentMapper mapper = (CommentMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{CommentMapper.class}, (proxy, method, args) -> {
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive(), method.getName());
                calls.add(method.getName());
                return switch (method.getName()) {
                    case "selectArticleStatus" -> articleStatus;
                    case "lockArticle" -> {
                        if (articleStatus == null) yield null;
                        TbArticle article = new TbArticle(); article.setArticleId(7);
                        article.setArticleTitle("Article"); article.setArticleStatus(articleStatus); yield article;
                    }
                    case "selectById" -> {
                        ArticleComment copy = copy(stored);
                        if (copy != null && staleInitialStatus != null) copy.setStatus(staleInitialStatus);
                        yield copy;
                    }
                    case "selectForUpdate" -> copy(stored);
                    case "selectRequestForUpdate" -> REQUEST.equals(storedRequest) ? copy(stored) : null;
                    case "insertPending" -> {
                        if (stored == null) {
                            stored = comment("PENDING"); stored.setContent((String) args[2]); storedRequest = (String) args[3];
                        }
                        yield 1;
                    }
                    case "updateStatus" -> {
                        stored.setStatus((String) args[1]); stored.setModeratedBy((Long) args[2]);
                        stored.setModeratedAt(new Date()); yield 1;
                    }
                    case "changeApprovedCount" -> {
                        if (failCount) throw new IllegalStateException("DB unavailable");
                        approved += (Integer) args[1]; yield 1;
                    }
                    case "countComments" -> { lastCount = args; yield 103L; }
                    case "selectComments" -> { lastPage = args; yield List.of(comment("APPROVED")); }
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
    private final TbUserMapper users = (TbUserMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{TbUserMapper.class}, (proxy, method, args) -> {
                assertEquals("selectByPrimaryKey", method.getName());
                calls.add("user");
                if (Long.valueOf(999).equals(args[0])) return null;
                TbUser user = new TbUser(); user.setId((Long) args[0]); user.setUsername("Reader");
                user.setRole(Long.valueOf(91).equals(args[0]) && admin ? "ADMIN" : "USER"); return user;
            });
    private final TestTransactions transactions = new TestTransactions();
    private final ArticleCacheInvalidationPublisher publisher = new ArticleCacheInvalidationPublisher(null, "test") {
        @Override public void publishAfterCommit() {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    events++;
                    if (failEvent) throw new IllegalStateException("Committed but invalidation failed");
                }
            });
        }
    };
    private final CommentServiceImpl service = new CommentServiceImpl(mapper, users, publisher, transactions);

    @Test void invalidInputCannotTouchDatabaseOrStartTransactions() {
        assertThrows(IllegalArgumentException.class, () -> service.submitComment(0, 26, "hello", REQUEST));
        assertThrows(IllegalArgumentException.class, () -> service.submitComment(7, 0, "hello", REQUEST));
        for (String text : new String[]{null, " \n", "bad\0text", "😀".repeat(1001)}) {
            assertThrows(IllegalArgumentException.class, () -> service.submitComment(7, 26, text, REQUEST));
        }
        assertThrows(IllegalArgumentException.class, () -> service.submitComment(7, 26, "hello", "bad"));
        assertThrows(IllegalArgumentException.class, () -> service.getPublishedComments(7, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.getOwnComments(7, 26, 1, 51));
        assertThrows(IllegalArgumentException.class, () -> service.getModerationQueue("SQL", null, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.moderateComment(42, 91, "PENDING"));
        assertTrue(calls.isEmpty()); assertEquals(0, commits);
    }

    @Test void submissionIsPendingPlainTextNormalizedAndIdempotentWithoutPublishingAnEvent() {
        ArticleComment first = service.submitComment(7, 26, "  <script>alert(1)</script>\r\nhello\rworld  ", REQUEST);
        assertEquals("<script>alert(1)</script>\nhello\nworld", first.getContent());
        assertEquals("PENDING", first.getStatus()); assertEquals("Reader", first.getUsername());
        assertEquals("Article", first.getArticleTitle()); assertNull(first.getModeratedBy());
        ArticleComment repeated = service.submitComment(7, 26, first.getContent(), REQUEST);
        assertEquals(first.getId(), repeated.getId()); assertEquals(0, approved); assertEquals(0, events);
        assertThrows(IllegalArgumentException.class, () -> service.submitComment(7, 26, "different", REQUEST));
        assertEquals(first.getContent(), stored.getContent());
        assertTrue(calls.indexOf("lockArticle") < calls.indexOf("insertPending"));
    }

    @Test void deletedSubmissionRetryReturnsItsTerminalStateAndCannotCreateAnotherComment() {
        stored = comment("DELETED"); storedRequest = REQUEST;
        assertEquals("DELETED", service.submitComment(7, 26, stored.getContent(), REQUEST).getStatus());
        assertEquals(0, approved); assertEquals(0, events);
    }

    @Test void acceptsOneThousandUnicodeCharactersAndRejectsMissingAccounts() {
        assertEquals(1000, service.submitComment(7, 26, "😀".repeat(1000), REQUEST).getContent().codePointCount(0, 2000));
        assertThrows(IllegalArgumentException.class, () -> service.submitComment(7, 999, "hello", REQUEST));
    }

    @Test void unpublishedArticlesRejectPublicReadsSubmissionsAndAuthorDeletion() {
        stored = comment("APPROVED"); approved = 1;
        for (String status : new String[]{null, "DRAFT", "OFFLINE"}) {
            articleStatus = status;
            assertNull(service.getPublishedComments(7, 1, 10));
            assertNull(service.getOwnComments(7, 26, 1, 10));
            assertNull(service.submitComment(7, 26, "hello", REQUEST));
            assertNull(service.deleteOwnComment(42, 26));
        }
        assertEquals(1, approved); assertEquals("APPROVED", stored.getStatus()); assertEquals(0, events);
    }

    @Test void publicAndOwnQueriesUseDistinctFiltersAndLongPaginationOffsets() {
        var page = service.getPublishedComments(7, 2, 10);
        assertArrayEquals(new Object[]{7, null, "APPROVED"}, lastCount);
        assertArrayEquals(new Object[]{7, null, "APPROVED", 10L, 10}, lastPage);
        assertEquals(103L, page.getTotal()); assertEquals(11, page.getPages());
        assertInstanceOf(ArrayList.class, page.getComments());
        service.getOwnComments(7, 26, Integer.MAX_VALUE, 50);
        assertArrayEquals(new Object[]{7, 26L, null}, lastCount);
        assertEquals(((long) Integer.MAX_VALUE - 1) * 50, lastPage[3]);
    }

    @Test void moderationQueueDefaultsPendingAndSupportsAllWithoutAnAuthorFilter() {
        service.getModerationQueue(null, null, 1, 20);
        assertArrayEquals(new Object[]{null, null, "PENDING"}, lastCount);
        service.getModerationQueue("ALL", 7, 1, 20);
        assertArrayEquals(new Object[]{7, null, null}, lastCount);
    }

    @Test void moderationTransitionsCountOnlyApprovalAndDeletedStateIsTerminal() {
        stored = comment("PENDING");
        assertEquals("APPROVED", service.moderateComment(42, 91, "APPROVED").getStatus());
        assertEquals(1, approved); assertEquals(91L, stored.getModeratedBy());
        service.moderateComment(42, 91, "APPROVED"); assertEquals(1, approved);
        service.moderateComment(42, 91, "REJECTED"); assertEquals(0, approved);
        service.moderateComment(42, 91, "APPROVED"); assertEquals(1, approved);
        service.moderateComment(42, 91, "DELETED"); assertEquals(0, approved);
        service.moderateComment(42, 91, "DELETED"); assertEquals(0, approved);
        assertThrows(IllegalArgumentException.class, () -> service.moderateComment(42, 91, "APPROVED"));
        assertEquals(6, events);
        assertTrue(calls.indexOf("lockArticle") < calls.indexOf("selectForUpdate"));
    }

    @Test void ordinaryUsersAndRevokedAdministratorsCannotModerate() {
        stored = comment("PENDING");
        assertThrows(IllegalArgumentException.class, () -> service.moderateComment(42, 26, "APPROVED"));
        admin = false;
        assertThrows(IllegalArgumentException.class, () -> service.moderateComment(42, 91, "APPROVED"));
        assertEquals("PENDING", stored.getStatus()); assertEquals(0, events);
    }

    @Test void authorCanOnlyDeleteOwnCommentsAndRepeatedDeletionDoesNotDecrementAgain() {
        stored = comment("APPROVED"); approved = 1;
        assertNull(service.deleteOwnComment(42, 91)); assertEquals(1, approved);
        assertEquals("DELETED", service.deleteOwnComment(42, 26).getStatus());
        assertEquals(0, approved); assertNull(stored.getModeratedBy());
        assertEquals("DELETED", service.deleteOwnComment(42, 26).getStatus());
        assertEquals(0, approved); assertEquals(2, events);
    }

    @Test void sameStateRetryReturnsTheCurrentLockingReadInsteadOfAnOldSnapshot() {
        stored = comment("APPROVED"); approved = 1; staleInitialStatus = "PENDING";
        assertEquals("APPROVED", service.moderateComment(42, 91, "APPROVED").getStatus());
        assertEquals(1, approved); assertEquals(1, events);
    }

    @Test void failedCounterWriteRollsBackStateAndDoesNotPublish() {
        stored = comment("PENDING"); failCount = true;
        assertThrows(IllegalStateException.class, () -> service.moderateComment(42, 91, "APPROVED"));
        assertEquals("PENDING", stored.getStatus()); assertEquals(0, approved);
        assertEquals(1, rollbacks); assertEquals(0, commits); assertEquals(0, events);
    }

    @Test void invalidationFailureIsAfterCommitAndIdempotentRetryResendsIt() {
        stored = comment("PENDING"); failEvent = true;
        assertThrows(IllegalStateException.class, () -> service.moderateComment(42, 91, "APPROVED"));
        assertEquals("APPROVED", stored.getStatus()); assertEquals(1, approved);
        assertEquals(1, commits); assertEquals(0, rollbacks);
        failEvent = false;
        service.moderateComment(42, 91, "APPROVED");
        assertEquals(1, approved); assertEquals(2, events); assertEquals(2, commits);
    }

    private static ArticleComment comment(String status) {
        ArticleComment c = new ArticleComment(); c.setId(42L); c.setArticleId(7); c.setUserId(26L);
        c.setContent("hello"); c.setStatus(status); c.setUsername("Reader"); c.setArticleTitle("Article"); c.setCreated(new Date(1));
        return c;
    }
    private static ArticleComment copy(ArticleComment original) {
        if (original == null) return null;
        ArticleComment c = comment(original.getStatus()); c.setContent(original.getContent());
        c.setModeratedBy(original.getModeratedBy()); c.setModeratedAt(original.getModeratedAt()); return c;
    }
    private final class TestTransactions extends AbstractPlatformTransactionManager {
        private ArticleComment before;
        private String beforeRequest;
        private int beforeCount;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            before = copy(stored); beforeCount = approved; beforeRequest = storedRequest;
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            stored = before; approved = beforeCount; storedRequest = beforeRequest; rollbacks++;
        }
    }
}
