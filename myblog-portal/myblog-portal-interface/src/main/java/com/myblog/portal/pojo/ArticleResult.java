package com.myblog.portal.pojo;

import com.myblog.pojo.TbArticle;

import java.io.Serializable;
import java.util.List;

public class ArticleResult implements Serializable {

	private boolean hasPreviousPage;//是否有前一页
    private boolean hasNextPage;    //是否有后一页
    private int pageNum;            //当前页
    private int pages;              //总页数
    private int[] navigatepageNums;   //所有导航号
	private List<TbArticle> articleList;

    public List<TbArticle> getArticleList() {
        return articleList;
    }

    public void setArticleList(List<TbArticle> articleList) {
        this.articleList = articleList;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int[] getNavigatepageNums() {
        return navigatepageNums;
    }

    public void setNavigatepageNums(int[] navigatepageNums) {
        this.navigatepageNums = navigatepageNums;
    }

}
