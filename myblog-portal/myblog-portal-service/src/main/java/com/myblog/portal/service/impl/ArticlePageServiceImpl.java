package com.myblog.portal.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.myblog.portal.pojo.ArticleResult;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.portal.service.ArticlePageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticlePageServiceImpl implements ArticlePageService {

    @Autowired
    private TbArticleMapper tbArticleMapper;

    @Override
    public ArticleResult getPortalArticleListPage(int pageNum, int pageSize, String orderBy) {


        //引入pagehelper插件，
        //设置分页信息
        PageHelper.startPage(pageNum,pageSize);
        //执行分页查询
        //根据排序参数执行相应的分页查询
        List<TbArticle> tbArticleList=new ArrayList<>();
        if ("heat".equals(orderBy)){
            tbArticleList=tbArticleMapper.selectAllWithTextOrderByHeat();
        }
        if ("new".equals(orderBy)){
            tbArticleList=tbArticleMapper.selectAllWithTextOrderByNew();
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


        return articleResult;
    }

    @Override
    public TbArticle getArticleWithText(int articleId) {
        return tbArticleMapper.selectByPrimaryKey(articleId);
    }
}
