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
import org.springframework.beans.factory.annotation.Autowired;
import com.myblog.service.messaging.ArticleCacheInvalidationPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;

@Service
public class ArticleServiceImpl implements ArticleService {
    private static final ObjectMapper RPC_RESPONSE_MAPPER = new ObjectMapper();
    @Autowired
    private TbArticleMapper tbArticleMapper;
    @Autowired
    private ArticleCacheInvalidationPublisher cacheInvalidationPublisher;

    @Override
    public Result getArticleListPage(int pageNum, int pageSize, String orderBy) {
        if (pageNum < 1 || pageSize < 1 || (!"heat".equals(orderBy) && !"new".equals(orderBy))) {
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
        tbArticleMapper.deleteByPrimaryKey(articleId);
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }

    /**
     * 批量删除
     * @param articleIds id集合
     */
    @Override
    public void deleteBatch(List<Integer> articleIds) {
        TbArticleExample example=new TbArticleExample();
        TbArticleExample.Criteria criteria = example.createCriteria();
        criteria.andArticleIdIn(articleIds);
        tbArticleMapper.deleteByExample(example);
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }

    /**
     * 添加文章
     * @param tbArticle 文章
     */
    @Override
    public void addArticle(TbArticle tbArticle) {
        tbArticleMapper.insertSelective(tbArticle);
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
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
        tbArticleMapper.updateByPrimaryKeySelective(tbArticle);
        //发消息更新缓存,清空缓存
        cacheInvalidationPublisher.publishAfterCommit();
    }
}
