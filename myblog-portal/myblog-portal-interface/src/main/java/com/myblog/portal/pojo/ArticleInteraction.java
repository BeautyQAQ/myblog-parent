package com.myblog.portal.pojo;

import java.io.Serializable;

/** Public counters and the authenticated reader's current like state. */
public class ArticleInteraction implements Serializable {
    private static final long serialVersionUID = 1L;

    private int articleId;
    private int pageviews;
    private int likeCount;
    private int heat;
    private boolean liked;

    public int getArticleId() { return articleId; }
    public void setArticleId(int articleId) { this.articleId = articleId; }
    public int getPageviews() { return pageviews; }
    public void setPageviews(int pageviews) { this.pageviews = pageviews; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getHeat() { return heat; }
    public void setHeat(int heat) { this.heat = heat; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
}
