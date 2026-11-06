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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class ArticlePageServiceImpl implements ArticlePageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArticlePageServiceImpl.class);
    // Interactions are frequent: bound list/ranking staleness without invalidating every search per view.
    private static final int STATISTICS_CACHE_SECONDS = 30;

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
        return getPortalArticleListPage(pageNum, pageSize, orderBy, null, null);
    }

    @Override
    public ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy,
                                                 String keyword, String tag) {
        keyword = normalizeFilter(keyword);
        tag = normalizeFilter(tag);
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageNum must be positive and pageSize must be between 1 and 100");
        }
        if (!"heat".equals(orderBy) && !"new".equals(orderBy)) {
            throw new IllegalArgumentException("orderBy must be heat or new");
        }

        String cacheKey = null;
        try {
            cacheKey = cacheNamespace() + ":page:" + orderBy + ':' + pageNum + ':' + pageSize
                    + ':' + filterDigest(keyword, tag);
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
            String titlePattern = keyword == null ? null : "%" + keyword.replace("=", "==")
                    .replace("%", "=%").replace("_", "=_") + "%";
            List<TbArticle> articles = articleMapper.selectPublishedArticles(titlePattern, tag, orderBy);
            PageInfo<TbArticle> page = new PageInfo<>(articles);
            result = new ArticleResult();
            result.setHasNextPage(page.isHasNextPage());
            result.setHasPreviousPage(page.isHasPreviousPage());
            result.setArticleList(new ArrayList<>(page.getList()));
            result.setPageNum(page.getPageNum());
            result.setPages(page.getPages());
            result.setTotal(page.getTotal());
            result.setNavigatepageNums(page.getNavigatepageNums());
        } finally {
            PageHelper.clearPage();
        }

        if (cacheKey != null) {
            try {
                // SETEX keeps the value and its expiry atomic, including during Redis failures.
                jedisClient.setex(cacheKey, Math.min(cacheExpire, STATISTICS_CACHE_SECONDS), JsonUtils.objectToJson(result));
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to cache article page {}", cacheKey, e);
            }
        }
        return result;
    }

    @Override
    public List<String> getPublishedTags() {
        String cacheKey = null;
        try {
            cacheKey = cacheNamespace() + ":tags";
            String json = jedisClient.get(cacheKey);
            if (json != null && !json.isBlank()) {
                List<String> tags = JsonUtils.jsonToList(json, String.class);
                if (tags != null) return new ArrayList<>(tags);
            }
        } catch (RuntimeException e) {
            cacheKey = null;
            LOGGER.warn("Unable to read article tags cache; querying MySQL", e);
        }
        List<String> tags;
        try {
            tags = new ArrayList<>(articleMapper.selectPublishedTags());
        } finally {
            PageHelper.clearPage();
        }
        if (cacheKey != null) {
            try {
                jedisClient.setex(cacheKey, cacheExpire, JsonUtils.objectToJson(tags));
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to cache article tags", e);
            }
        }
        return tags;
    }

    private String cacheNamespace() {
        String version = jedisClient.get(cachePrefix + ":version");
        // Do not reuse pre-interaction pages, which may have a much longer TTL.
        return cachePrefix + ":published:interactions:search:v" + (version == null ? "0" : version);
    }

    private static String normalizeFilter(String value) {
        if (value == null) return null;
        value = value.strip();
        if (value.codePointCount(0, value.length()) > 50) {
            throw new IllegalArgumentException("搜索关键词和标签不能超过 50 个字符");
        }
        return value.isEmpty() ? null : value;
    }

    private static String filterDigest(String keyword, String tag) {
        String first = keyword == null ? "" : keyword;
        String second = tag == null ? "" : tag;
        // Length prefixes keep query/tag boundaries unambiguous, including literal ':' characters.
        String filters = first.length() + ":" + first + second.length() + ":" + second;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(filters.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public TbArticle getArticleWithText(int articleId) {
        if (articleId < 1) {
            throw new IllegalArgumentException("articleId must be positive");
        }
        return articleMapper.selectPublishedByPrimaryKey(articleId);
    }
}
