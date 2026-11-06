package com.myblog.pojo;

import java.io.Serializable;
import java.util.Date;

public class ArticleComment implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Integer articleId;
    private Long userId;
    private Long moderatedBy;
    private String username;
    private String articleTitle;
    private String content;
    private String status;
    private Date created;
    private Date moderatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Integer getArticleId() { return articleId; }
    public void setArticleId(Integer value) { articleId = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public Long getModeratedBy() { return moderatedBy; }
    public void setModeratedBy(Long value) { moderatedBy = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getArticleTitle() { return articleTitle; }
    public void setArticleTitle(String value) { articleTitle = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Date getCreated() { return created; }
    public void setCreated(Date value) { created = value; }
    public Date getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(Date value) { moderatedAt = value; }
}
