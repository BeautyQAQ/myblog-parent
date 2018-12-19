package com.myblog.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.myblog.common.utils.DateTimeUtil;
import com.myblog.common.utils.Result;
import com.myblog.mapper.TbArticleMapper;
import com.myblog.pojo.TbArticle;
import com.myblog.pojo.TbArticleExample;
import com.myblog.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private TbArticleMapper tbArticleMapper;


    @Override
    public Result getArticleListPage(int pageNum, int pageSize, String orderBy) {
        //引入pagehelper插件，
        //设置分页信息
        PageHelper.startPage(pageNum,pageSize);
        //执行分页查询
        //根据排序参数执行相应的分页查询
        List<TbArticle> tbArticleList=new ArrayList<>();
        if ("heat".equals(orderBy)){
            tbArticleList=tbArticleMapper.selectAllOrderByHeat();
        }
        if ("new".equals(orderBy)){
            tbArticleList=tbArticleMapper.selectAllOrderByNew();
        }
        //使用pageinfo包装分页结果，返回分页结果
        PageInfo<TbArticle> tbArticlePageInfo = new PageInfo<TbArticle>(tbArticleList);
        return Result.ok(tbArticlePageInfo);
    }

    /**
     * 单个删除
     * @param articleId id
     */
    @Override
    public void deleteArticle(int articleId) {
        tbArticleMapper.deleteByPrimaryKey(articleId);
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
    }

    /**
     * 添加文章
     * @param tbArticle 文章
     */
    @Override
    public void addArticle(TbArticle tbArticle) {
        tbArticleMapper.insertSelective(tbArticle);
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
    }
}
