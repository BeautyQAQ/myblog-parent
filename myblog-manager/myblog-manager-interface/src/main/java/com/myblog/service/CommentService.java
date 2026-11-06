package com.myblog.service;

import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.CommentPage;

public interface CommentService {
    CommentPage getPublishedComments(int articleId, int pageNum, int pageSize);
    CommentPage getOwnComments(int articleId, long userId, int pageNum, int pageSize);
    ArticleComment submitComment(int articleId, long userId, String content, String requestId);
    ArticleComment deleteOwnComment(long commentId, long userId);
    CommentPage getModerationQueue(String status, Integer articleId, int pageNum, int pageSize);
    ArticleComment moderateComment(long commentId, long adminId, String status);
}
