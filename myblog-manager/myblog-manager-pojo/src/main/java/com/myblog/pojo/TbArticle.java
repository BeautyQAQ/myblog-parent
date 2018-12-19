package com.myblog.pojo;

import java.io.Serializable;
import java.util.Date;

public class TbArticle implements Serializable {
    private Integer articleId;

    private String articleTitle;

    private String articleTag;

    private Integer articleAuthorId;

    private String articleImageTitle;

    private Integer articlePageviews;

    private Integer articleLikecount;

    private Integer articleComment;

    private Integer articleHeat;

    private Date created;

    private Date updated;

    private String articleText;

    public Integer getArticleId() {
        return articleId;
    }

    public void setArticleId(Integer articleId) {
        this.articleId = articleId;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle == null ? null : articleTitle.trim();
    }

    public String getArticleTag() {
        return articleTag;
    }

    public void setArticleTag(String articleTag) {
        this.articleTag = articleTag == null ? null : articleTag.trim();
    }

    public Integer getArticleAuthorId() {
        return articleAuthorId;
    }

    public void setArticleAuthorId(Integer articleAuthorId) {
        this.articleAuthorId = articleAuthorId;
    }

    public String getArticleImageTitle() {
        return articleImageTitle;
    }

    public void setArticleImageTitle(String articleImageTitle) {
        this.articleImageTitle = articleImageTitle == null ? null : articleImageTitle.trim();
    }

    public Integer getArticlePageviews() {
        return articlePageviews;
    }

    public void setArticlePageviews(Integer articlePageviews) {
        this.articlePageviews = articlePageviews;
    }

    public Integer getArticleLikecount() {
        return articleLikecount;
    }

    public void setArticleLikecount(Integer articleLikecount) {
        this.articleLikecount = articleLikecount;
    }

    public Integer getArticleComment() {
        return articleComment;
    }

    public void setArticleComment(Integer articleComment) {
        this.articleComment = articleComment;
    }

    public Integer getArticleHeat() {
        return articleHeat;
    }

    public void setArticleHeat(Integer articleHeat) {
        this.articleHeat = articleHeat;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public String getArticleText() {
        return articleText;
    }

    public void setArticleText(String articleText) {
        this.articleText = articleText == null ? null : articleText.trim();
    }
}