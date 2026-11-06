package com.myblog.portal.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.myblog.common.jedis.JedisClient;
import com.myblog.common.utils.JsonUtils;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.pojo.ArticleResult;
import com.myblog.portal.service.ArticlePageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticlePageServiceImpl implements ArticlePageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticlePageServiceImpl.class);

    private final TbArticleMapper articleMapper;
    private final JedisClient jedisClient;
    private final int cacheExpire;
    private final String cachePrefix;

    public ArticlePageServiceImpl(TbArticleMapper articleMapper, JedisClient jedisClient,
                                  @Value("${ARTICLE_CACHE_EXPIRE}") int cacheExpire,
                                  @Value("${CONTENT_LIST}") String cachePrefix) {
        this.articleMapper = articleMapper;
        this.jedisClient = jedisClient;
        this.cacheExpire = cacheExpire;
        this.cachePrefix = cachePrefix;
    }

    @Override
    public ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageNum must be positive and pageSize must be between 1 and 100");
        }
        if (!"heat".equals(orderBy) && !"new".equals(orderBy)) {
            throw new IllegalArgumentException("orderBy must be heat or new");
        }

        String cacheKey = null;
        try {
            String version = jedisClient.get(cachePrefix + ":version");
            cacheKey = cachePrefix + ":published:v" + (version == null ? "0" : version)
                    + ':' + orderBy + ':' + pageNum + ':' + pageSize;
            String json = jedisClient.get(cacheKey);
            if (json != null && !json.isBlank()) {
                ArticleResult cached = JsonUtils.jsonToPojo(json, ArticleResult.class);
                if (cached != null) {
                    return cached;
                }
            }
        } catch (RuntimeException e) {
            // Fall back to MySQL; never write a cache entry without a known namespace.
            cacheKey = null;
            LOGGER.warn("Unable to read article cache; querying MySQL", e);
        }

        ArticleResult result;
        // Start pagination only for a real SELECT, and clear it even if the query fails.
        PageHelper.startPage(pageNum, pageSize);
        try {
            List<TbArticle> articles = "heat".equals(orderBy)
                    ? articleMapper.selectAllWithTextOrderByHeat()
                    : articleMapper.selectAllWithTextOrderByNew();
            PageInfo<TbArticle> page = new PageInfo<>(articles);
            result = new ArticleResult();
            result.setHasNextPage(page.isHasNextPage());
            result.setHasPreviousPage(page.isHasPreviousPage());
            result.setArticleList(new ArrayList<>(page.getList()));
            result.setPageNum(page.getPageNum());
            result.setPages(page.getPages());
            result.setNavigatepageNums(page.getNavigatepageNums());
        } finally {
            PageHelper.clearPage();
        }

        if (cacheKey != null) {
            try {
                // SETEX keeps the value and its expiry atomic, including during Redis failures.
                jedisClient.setex(cacheKey, cacheExpire, JsonUtils.objectToJson(result));
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to cache article page {}", cacheKey, e);
            }
        }
        return result;
    }

    @Override
    public TbArticle getArticleWithText(int articleId) {
        if (articleId < 1) {
            throw new IllegalArgumentException("articleId must be positive");
        }
        return articleMapper.selectPublishedByPrimaryKey(articleId);
    }
}
