package com.myblog.portal.service;

import com.myblog.portal.pojo.ArticleInteraction;

public interface ArticleInteractionService {
    /** Records at most one view per visitor and article in a rolling 30 minute window. */
    ArticleInteraction recordView(int articleId, String visitorKey, Long userId);

    /** Returns null for missing, draft and offline articles. */
    ArticleInteraction getInteraction(int articleId, Long userId);

    /** Sets the desired state, so repeated requests do not toggle or duplicate likes. */
    ArticleInteraction setLiked(int articleId, long userId, boolean liked);
}
