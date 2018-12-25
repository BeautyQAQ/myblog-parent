package com.myblog.portal.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.myblog.common.jedis.JedisClient;
import com.myblog.common.utils.JsonUtils;
import com.myblog.portal.pojo.ArticleResult;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.service.ArticlePageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticlePageServiceImpl implements ArticlePageService {

    @Autowired
    private TbArticleMapper tbArticleMapper;
    @Autowired
    private JedisClient jedisClient;
    @Value("${ARTICLE_CACHE_EXPIRE}")
    private Integer ARTICLE_CACHE_EXPIRE;
    @Value("${CONTENT_LIST}")
    private String CONTENT_LIST;

    @Override
    public ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy) {
        //引入pagehelper插件，
        //设置分页信息
        PageHelper.startPage(pageNum,pageSize);
        //执行分页查询
        //根据排序参数执行相应的分页查询

        //先查缓存
        try {
            String json = jedisClient.get(CONTENT_LIST);
            if (StringUtils.isNotBlank(json)){
                ArticleResult articleResult = JsonUtils.jsonToPojo(json, ArticleResult.class);
                System.out.println("从缓存中查询");
                return articleResult;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        //缓存中没有，就查数据库
        List<TbArticle> tbArticleList=new ArrayList<>();
        if ("heat".equals(orderBy)){
            tbArticleList=tbArticleMapper.selectAllWithTextOrderByHeat();
            System.out.println("从数据库中查询");
        }
        if ("new".equals(orderBy)){
            tbArticleList=tbArticleMapper.selectAllWithTextOrderByNew();
            System.out.println("从数据库中查询");
        }
        //使用pageinfo包装分页结果
        PageInfo<TbArticle> tbArticlePageInfo = new PageInfo<TbArticle>(tbArticleList);
        ArticleResult articleResult = new ArticleResult();
        articleResult.setHasNextPage(tbArticlePageInfo.isHasNextPage());
        articleResult.setHasPreviousPage(tbArticlePageInfo.isHasPreviousPage());
        articleResult.setArticleList(tbArticlePageInfo.getList());
        articleResult.setPageNum(tbArticlePageInfo.getPageNum());
        articleResult.setPages(tbArticlePageInfo.getPages());
        articleResult.setNavigatepageNums(tbArticlePageInfo.getNavigatepageNums());

        //将数据库分页查询结果放入缓存
        try {
            jedisClient.set(CONTENT_LIST, JsonUtils.objectToJson(articleResult));
            //设置过期时间
            jedisClient.expire(CONTENT_LIST, ARTICLE_CACHE_EXPIRE);
            System.out.println("将数据库数据放入缓存中");
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return articleResult;
    }

    @Override
    public TbArticle getArticleWithText(int articleId) {
        return tbArticleMapper.selectByPrimaryKey(articleId);
    }
}
