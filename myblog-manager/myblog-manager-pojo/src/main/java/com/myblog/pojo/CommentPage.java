package com.myblog.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CommentPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<ArticleComment> comments = new ArrayList<>();
    private int pageNum;
    private int pageSize;
    private int pages;
    private long total;
    public List<ArticleComment> getComments() { return comments; }
    public void setComments(List<ArticleComment> value) { comments = value; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int value) { pageNum = value; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int value) { pageSize = value; }
    public int getPages() { return pages; }
    public void setPages(int value) { pages = value; }
    public long getTotal() { return total; }
    public void setTotal(long value) { total = value; }
}
