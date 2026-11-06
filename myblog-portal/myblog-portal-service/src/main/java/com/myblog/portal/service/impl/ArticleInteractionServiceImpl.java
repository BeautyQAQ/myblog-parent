package com.myblog.portal.service.impl;

import com.myblog.mapper.ArticleInteractionMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.pojo.ArticleInteraction;
import com.myblog.portal.service.ArticleInteractionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ArticleInteractionServiceImpl implements ArticleInteractionService {
    private final ArticleInteractionMapper mapper;
    private final TransactionTemplate transactions;

    public ArticleInteractionServiceImpl(ArticleInteractionMapper mapper, PlatformTransactionManager transactionManager) {
        this.mapper = mapper;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public ArticleInteraction recordView(int articleId, String visitorKey, Long userId) {
        validate(articleId, userId);
        if (visitorKey == null || !visitorKey.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("无效的访客标识");
        }
        return transactions.execute(status -> {
            TbArticle article = mapper.selectPublishedForUpdate(articleId);
            if (article == null) return null;
            if (!mapper.hasRecentView(articleId, visitorKey)) {
                // Use the prior timestamp, never driver-dependent UPSERT affected-row semantics.
                mapper.saveView(articleId, visitorKey);
                mapper.incrementViews(articleId);
                article = mapper.selectPublishedForUpdate(articleId);
            }
            return result(article, userId != null && mapper.hasLike(articleId, userId));
        });
    }

    @Override
    public ArticleInteraction getInteraction(int articleId, Long userId) {
        validate(articleId, userId);
        return transactions.execute(status -> {
            TbArticle article = mapper.selectPublishedForUpdate(articleId);
            return article == null ? null : result(article, userId != null && mapper.hasLike(articleId, userId));
        });
    }

    @Override
    public ArticleInteraction setLiked(int articleId, long userId, boolean liked) {
        validate(articleId, userId);
        return transactions.execute(status -> {
            TbArticle article = mapper.selectPublishedForUpdate(articleId);
            if (article == null) return null;
            boolean existing = mapper.hasLike(articleId, userId);
            if (existing != liked) {
                int affected = liked ? mapper.insertLike(articleId, userId) : mapper.deleteLike(articleId, userId);
                if (affected != 1) throw new IllegalStateException("点赞状态更新失败");
                mapper.changeLikeCount(articleId, liked ? 1 : -1);
                article = mapper.selectPublishedForUpdate(articleId);
            }
            return result(article, liked);
        });
    }

    private static void validate(int articleId, Long userId) {
        if (articleId < 1 || (userId != null && userId < 1)) {
            throw new IllegalArgumentException("无效的文章或用户");
        }
    }

    private static ArticleInteraction result(TbArticle article, boolean liked) {
        ArticleInteraction result = new ArticleInteraction();
        result.setArticleId(article.getArticleId());
        result.setPageviews(article.getArticlePageviews() == null ? 0 : article.getArticlePageviews());
        result.setLikeCount(article.getArticleLikecount());
        result.setHeat(article.getArticleHeat());
        result.setLiked(liked);
        return result;
    }
}
