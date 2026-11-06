package com.myblog.mapper;

import com.myblog.pojo.TbArticle;
import org.apache.ibatis.annotations.Param;

public interface ArticleInteractionMapper {
    TbArticle selectPublishedForUpdate(int articleId);

    boolean hasRecentView(@Param("articleId") int articleId, @Param("visitorKey") String visitorKey);
    int saveView(@Param("articleId") int articleId, @Param("visitorKey") String visitorKey);
    int incrementViews(int articleId);

    boolean hasLike(@Param("articleId") int articleId, @Param("userId") long userId);
    int insertLike(@Param("articleId") int articleId, @Param("userId") long userId);
    int deleteLike(@Param("articleId") int articleId, @Param("userId") long userId);
    int changeLikeCount(@Param("articleId") int articleId, @Param("delta") int delta);
}
