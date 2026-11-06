package com.myblog.mapper;

import com.myblog.pojo.ArticleComment;
import com.myblog.pojo.TbArticle;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CommentMapper {
    String selectArticleStatus(@Param("articleId") int articleId);
    TbArticle lockArticle(@Param("articleId") int articleId);
    ArticleComment selectById(@Param("id") long id);
    ArticleComment selectForUpdate(@Param("id") long id);
    ArticleComment selectRequestForUpdate(@Param("userId") long userId, @Param("requestId") String requestId);
    int insertPending(@Param("articleId") int articleId, @Param("userId") long userId,
                      @Param("content") String content, @Param("requestId") String requestId);
    int updateStatus(@Param("id") long id, @Param("status") String status, @Param("moderatedBy") Long moderatedBy);
    int changeApprovedCount(@Param("articleId") int articleId, @Param("delta") int delta);
    long countComments(@Param("articleId") Integer articleId, @Param("userId") Long userId, @Param("status") String status);
    List<ArticleComment> selectComments(@Param("articleId") Integer articleId, @Param("userId") Long userId,
                                       @Param("status") String status, @Param("offset") long offset, @Param("limit") int limit);
}
