package com.myblog.pojo;

import java.util.Date;

public class TbUserlog {
    private Integer id;

    private Integer userId;

    private Integer action;

    private Date crated;

    private Date updated;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public Date getCrated() {
        return crated;
    }

    public void setCrated(Date crated) {
        this.crated = crated;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}