package com.myblog.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.pojo.TbArticleExample;
import com.myblog.service.ArticleService;
import com.myblog.service.content.ArticleHtmlSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import com.myblog.service.messaging.ArticleCacheInvalidationPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ArticleServiceImpl implements ArticleService {
    private static final ObjectMapper RPC_RESPONSE_MAPPER = new ObjectMapper();
    private static final Set<String> ARTICLE_STATUSES = Set.of("DRAFT", "PUBLISHED", "OFFLINE");
    @Autowired
    private TbArticleMapper tbArticleMapper;
    @Autowired
    private ArticleCacheInvalidationPublisher cacheInvalidationPublisher;

    @Override
    public Result getArticleListPage(int pageNum, int pageSize, String orderBy) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100 || (!"heat".equals(orderBy) && !"new".equals(orderBy))) {
            throw new IllegalArgumentException("Invalid article pagination");
        }
        PageHelper.startPage(pageNum, pageSize);
        try {
            List<TbArticle> articles = "heat".equals(orderBy)
                    ? tbArticleMapper.selectAllOrderByHeat()
                    : tbArticleMapper.selectAllOrderByNew();
            PageInfo<TbArticle> page = new PageInfo<>(articles);
            // Preserve the HTTP pagination fields without requiring PageHelper in the RPC consumer.
            return Result.ok(RPC_RESPONSE_MAPPER.convertValue(page,
                    new TypeReference<LinkedHashMap<String, Object>>() {}));
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 单个删除
     * @param articleId id
     */
    @Override
    public void deleteArticle(int articleId) {
        if (articleId < 1 || tbArticleMapper.deleteByPrimaryKey(articleId) != 1) {
            throw new IllegalArgumentException("文章不存在");
        }
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }

    /**
     * 批量删除
     * @param articleIds id集合
     */
    @Override
    public void deleteBatch(List<Integer> articleIds) {
        if (articleIds == null || articleIds.isEmpty()
                || articleIds.stream().anyMatch(id -> id == null || id < 1)) {
            throw new IllegalArgumentException("请选择有效的文章");
        }
        List<Integer> ids = List.copyOf(new LinkedHashSet<>(articleIds));
        TbArticleExample example=new TbArticleExample();
        TbArticleExample.Criteria criteria = example.createCriteria();
        criteria.andArticleIdIn(ids);
        if (tbArticleMapper.countByExample(example) != ids.size()) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (tbArticleMapper.deleteByExample(example) != ids.size()) {
            throw new IllegalArgumentException("文章不存在");
        }
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }

    /**
     * 添加文章
     * @param tbArticle 文章
     */
    @Override
    public void addArticle(TbArticle tbArticle) {
        if (tbArticle == null) {
            throw new IllegalArgumentException("文章不能为空");
        }
        tbArticle.setArticleTitle(validTitle(tbArticle.getArticleTitle()));
        tbArticle.setArticleTag(validTag(tbArticle.getArticleTag()));
        tbArticle.setArticleText(ArticleHtmlSanitizer.clean(tbArticle.getArticleText()));
        tbArticle.setArticleStatus(validStatus(tbArticle.getArticleStatus(), "DRAFT"));
        Date now = new Date();
        tbArticle.setArticleId(null);
        tbArticle.setCreated(now);
        tbArticle.setUpdated(now);
        tbArticle.setPublishedAt("PUBLISHED".equals(tbArticle.getArticleStatus()) ? now : null);
        tbArticle.setArticlePageviews(0);
        tbArticle.setArticleLikecount(0);
        tbArticle.setArticleComment(0);
        tbArticle.setArticleHeat(0);
        if (tbArticleMapper.insertSelective(tbArticle) != 1) {
            throw new IllegalStateException("文章保存失败");
        }
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }

    @Override
    public TbArticle createArticle(TbArticle tbArticle) {
        addArticle(tbArticle);
        return tbArticle;
    }

    /**
     * 获取文章信息
     * @param articleId 文章id
     * @return
     */
    @Override
    public TbArticle getArticle(int articleId) {
        return tbArticleMapper.selectByPrimaryKey(articleId);
    }

    /**
     * 更新文章
     * @param tbArticle 文章
     */
    @Override
    public void updateArticle(TbArticle tbArticle) {
        if (tbArticle == null || tbArticle.getArticleId() == null || tbArticle.getArticleId() < 1) {
            throw new IllegalArgumentException("文章不存在");
        }
        TbArticle existing = tbArticleMapper.selectByPrimaryKey(tbArticle.getArticleId());
        if (existing == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        // Only editorial fields are written, so client-bound author/statistics/timestamps cannot overwrite them.
        TbArticle changes = new TbArticle();
        changes.setArticleId(existing.getArticleId());
        changes.setArticleTitle(validTitle(tbArticle.getArticleTitle() == null
                ? existing.getArticleTitle() : tbArticle.getArticleTitle()));
        changes.setArticleTag(validTag(tbArticle.getArticleTag() == null
                ? existing.getArticleTag() : tbArticle.getArticleTag()));
        changes.setArticleText(ArticleHtmlSanitizer.clean(tbArticle.getArticleText() == null
                ? existing.getArticleText() : tbArticle.getArticleText()));
        changes.setArticleImageTitle(tbArticle.getArticleImageTitle());
        changes.setArticleStatus(validStatus(tbArticle.getArticleStatus(), existing.getArticleStatus()));
        Date now = new Date();
        changes.setUpdated(now);
        if (existing.getPublishedAt() == null && "PUBLISHED".equals(changes.getArticleStatus())) {
            changes.setPublishedAt(now);
        }
        if (tbArticleMapper.updateByPrimaryKeySelective(changes) != 1) {
            throw new IllegalArgumentException("文章不存在");
        }
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }

    private static String validTitle(String title) {
        String value = title == null ? "" : title.strip();
        if (value.isEmpty() || value.codePointCount(0, value.length()) > 50) {
            throw new IllegalArgumentException("标题长度必须为 1 到 50 个字符");
        }
        return value;
    }

    private static String validTag(String tag) {
        if (tag != null && tag.codePointCount(0, tag.length()) > 50) {
            throw new IllegalArgumentException("标签不能超过 50 个字符");
        }
        return tag;
    }

    private static String validStatus(String status, String fallback) {
        String value = status == null ? fallback : status;
        if (!ARTICLE_STATUSES.contains(value == null ? "" : value)) {
            throw new IllegalArgumentException("无效的文章状态");
        }
        return value;
    }
}
