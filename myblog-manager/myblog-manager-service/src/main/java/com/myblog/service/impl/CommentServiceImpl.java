package com.myblog.service.impl;

import com.myblog.mapper.CommentMapper;
import com.myblog.mapper.TbUserMapper;
import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.CommentPage;
import com.myblog.pojo.TbUser;
import com.myblog.pojo.TbArticle;
import com.myblog.service.CommentService;
import com.myblog.service.messaging.ArticleCacheInvalidationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CommentServiceImpl implements CommentService {
    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "DELETED", "ALL");
    private static final Pattern REQUEST_ID = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    private final CommentMapper mapper;
    private final TbUserMapper users;
    private final ArticleCacheInvalidationPublisher invalidation;
    private final TransactionTemplate transactions;

    public CommentServiceImpl(CommentMapper mapper, TbUserMapper users,
                              ArticleCacheInvalidationPublisher invalidation, PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.users = users;
        this.invalidation = invalidation;
        transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public CommentPage getPublishedComments(int articleId, int pageNum, int pageSize) {
        positive(articleId);
        pagination(pageNum, pageSize);
        return transactions.execute(tx -> "PUBLISHED".equals(mapper.selectArticleStatus(articleId))
                ? page(articleId, null, "APPROVED", pageNum, pageSize) : null);
    }

    @Override
    public CommentPage getOwnComments(int articleId, long userId, int pageNum, int pageSize) {
        positive(articleId); positive(userId); pagination(pageNum, pageSize);
        return transactions.execute(tx -> "PUBLISHED".equals(mapper.selectArticleStatus(articleId))
                ? page(articleId, userId, null, pageNum, pageSize) : null);
    }

    @Override
    public ArticleComment submitComment(int articleId, long userId, String content, String requestId) {
        positive(articleId); positive(userId);
        String normalized = normalizeContent(content);
        if (requestId == null || !REQUEST_ID.matcher(requestId).matches()) {
            throw new IllegalArgumentException("请求标识无效，请刷新页面后重试");
        }
        return transactions.execute(tx -> {
            TbArticle article = mapper.lockArticle(articleId);
            if (article == null || !"PUBLISHED".equals(article.getArticleStatus())) return null;
            TbUser user = users.selectByPrimaryKey(userId);
            if (user == null) throw new IllegalArgumentException("用户不存在");
            // Insert first: locking a missing request key before inserting creates avoidable gap-lock deadlocks.
            // The unique key safely joins concurrent retries, without overwriting their payload or status.
            mapper.insertPending(articleId, userId, normalized, requestId);
            ArticleComment existing = mapper.selectRequestForUpdate(userId, requestId);
            if (existing == null) throw new IllegalStateException("评论提交失败");
            if (existing.getArticleId() != articleId || !normalized.equals(existing.getContent())) {
                throw new IllegalArgumentException("同一请求标识不能提交不同的评论，请刷新后重试");
            }
            existing.setUsername(user.getUsername());
            existing.setArticleTitle(article.getArticleTitle());
            return existing;
        });
    }

    @Override
    public ArticleComment deleteOwnComment(long commentId, long userId) {
        positive(commentId); positive(userId);
        return transactions.execute(tx -> {
            ArticleComment initial = mapper.selectById(commentId);
            if (initial == null || !Long.valueOf(userId).equals(initial.getUserId())) return null;
            TbArticle article = mapper.lockArticle(initial.getArticleId());
            if (article == null || !"PUBLISHED".equals(article.getArticleStatus())) return null;
            ArticleComment comment = mapper.selectForUpdate(commentId);
            if (comment == null || !Long.valueOf(userId).equals(comment.getUserId())) return null;
            changeStatus(comment, "DELETED", null);
            // Repeating deletion also retries invalidation if the first response failed after commit.
            invalidation.publishAfterCommit();
            return withDisplay(mapper.selectForUpdate(commentId), initial, article);
        });
    }

    @Override
    public CommentPage getModerationQueue(String status, Integer articleId, int pageNum, int pageSize) {
        if (articleId != null) positive(articleId);
        pagination(pageNum, pageSize);
        String selected = status == null || status.isBlank() ? "PENDING" : status;
        if (!STATUSES.contains(selected)) throw new IllegalArgumentException("无效的审核状态");
        return transactions.execute(tx -> page(articleId, null, "ALL".equals(selected) ? null : selected, pageNum, pageSize));
    }

    @Override
    public ArticleComment moderateComment(long commentId, long adminId, String status) {
        positive(commentId); positive(adminId);
        if (!Set.of("APPROVED", "REJECTED", "DELETED").contains(status == null ? "" : status)) {
            throw new IllegalArgumentException("无效的审核操作");
        }
        return transactions.execute(tx -> {
            TbUser admin = users.selectByPrimaryKey(adminId);
            if (admin == null || !TbUser.ROLE_ADMIN.equals(admin.getRole())) {
                throw new IllegalArgumentException("当前账号没有管理员权限");
            }
            ArticleComment initial = mapper.selectById(commentId);
            if (initial == null) return null;
            TbArticle article = mapper.lockArticle(initial.getArticleId());
            if (article == null) return null;
            ArticleComment comment = mapper.selectForUpdate(commentId);
            if (comment == null) return null;
            if ("DELETED".equals(comment.getStatus()) && !"DELETED".equals(status)) {
                throw new IllegalArgumentException("评论已删除，不能再次审核");
            }
            changeStatus(comment, status, adminId);
            invalidation.publishAfterCommit();
            return withDisplay(mapper.selectForUpdate(commentId), initial, article);
        });
    }

    private void changeStatus(ArticleComment comment, String status, Long moderator) {
        if (status.equals(comment.getStatus())) return;
        int delta = ("APPROVED".equals(status) ? 1 : 0) - ("APPROVED".equals(comment.getStatus()) ? 1 : 0);
        if (mapper.updateStatus(comment.getId(), status, moderator) != 1) throw new IllegalStateException("评论更新失败");
        if (delta != 0 && mapper.changeApprovedCount(comment.getArticleId(), delta) != 1) {
            throw new IllegalStateException("评论计数更新失败");
        }
    }

    private static ArticleComment withDisplay(ArticleComment comment, ArticleComment initial, TbArticle article) {
        // Return the locking read, never the earlier repeatable-read snapshot after a concurrent moderation.
        comment.setUsername(initial.getUsername());
        comment.setArticleTitle(article.getArticleTitle());
        return comment;
    }

    private CommentPage page(Integer articleId, Long userId, String status, int pageNum, int pageSize) {
        long total = mapper.countComments(articleId, userId, status);
        CommentPage page = new CommentPage();
        page.setPageNum(pageNum); page.setPageSize(pageSize); page.setTotal(total);
        page.setPages((int) Math.min(Integer.MAX_VALUE, (total + pageSize - 1) / pageSize));
        page.setComments(new ArrayList<>(mapper.selectComments(articleId, userId, status,
                ((long) pageNum - 1) * pageSize, pageSize)));
        return page;
    }

    private static void positive(long value) {
        if (value < 1) throw new IllegalArgumentException("无效的文章、评论或用户编号");
    }

    private static void pagination(int pageNum, int pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 50) throw new IllegalArgumentException("分页参数有误，每页最多50条");
    }

    private static String normalizeContent(String content) {
        String text = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (text.isBlank() || text.indexOf('\0') >= 0 || text.codePointCount(0, text.length()) > 1000) {
            throw new IllegalArgumentException("评论须为1到1000个字符，不能包含空字符");
        }
        return text;
    }
}
