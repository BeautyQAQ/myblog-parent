package com.myblog.pojo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TbArticleExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public TbArticleExample() {
        oredCriteria = new ArrayList<Criteria>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andArticleIdIsNull() {
            addCriterion("article_id is null");
            return (Criteria) this;
        }

        public Criteria andArticleIdIsNotNull() {
            addCriterion("article_id is not null");
            return (Criteria) this;
        }

        public Criteria andArticleIdEqualTo(Integer value) {
            addCriterion("article_id =", value, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdNotEqualTo(Integer value) {
            addCriterion("article_id <>", value, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdGreaterThan(Integer value) {
            addCriterion("article_id >", value, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("article_id >=", value, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdLessThan(Integer value) {
            addCriterion("article_id <", value, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdLessThanOrEqualTo(Integer value) {
            addCriterion("article_id <=", value, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdIn(List<Integer> values) {
            addCriterion("article_id in", values, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdNotIn(List<Integer> values) {
            addCriterion("article_id not in", values, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdBetween(Integer value1, Integer value2) {
            addCriterion("article_id between", value1, value2, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleIdNotBetween(Integer value1, Integer value2) {
            addCriterion("article_id not between", value1, value2, "articleId");
            return (Criteria) this;
        }

        public Criteria andArticleTitleIsNull() {
            addCriterion("article_title is null");
            return (Criteria) this;
        }

        public Criteria andArticleTitleIsNotNull() {
            addCriterion("article_title is not null");
            return (Criteria) this;
        }

        public Criteria andArticleTitleEqualTo(String value) {
            addCriterion("article_title =", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleNotEqualTo(String value) {
            addCriterion("article_title <>", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleGreaterThan(String value) {
            addCriterion("article_title >", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleGreaterThanOrEqualTo(String value) {
            addCriterion("article_title >=", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleLessThan(String value) {
            addCriterion("article_title <", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleLessThanOrEqualTo(String value) {
            addCriterion("article_title <=", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleLike(String value) {
            addCriterion("article_title like", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleNotLike(String value) {
            addCriterion("article_title not like", value, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleIn(List<String> values) {
            addCriterion("article_title in", values, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleNotIn(List<String> values) {
            addCriterion("article_title not in", values, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleBetween(String value1, String value2) {
            addCriterion("article_title between", value1, value2, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTitleNotBetween(String value1, String value2) {
            addCriterion("article_title not between", value1, value2, "articleTitle");
            return (Criteria) this;
        }

        public Criteria andArticleTagIsNull() {
            addCriterion("article_tag is null");
            return (Criteria) this;
        }

        public Criteria andArticleTagIsNotNull() {
            addCriterion("article_tag is not null");
            return (Criteria) this;
        }

        public Criteria andArticleTagEqualTo(String value) {
            addCriterion("article_tag =", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagNotEqualTo(String value) {
            addCriterion("article_tag <>", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagGreaterThan(String value) {
            addCriterion("article_tag >", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagGreaterThanOrEqualTo(String value) {
            addCriterion("article_tag >=", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagLessThan(String value) {
            addCriterion("article_tag <", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagLessThanOrEqualTo(String value) {
            addCriterion("article_tag <=", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagLike(String value) {
            addCriterion("article_tag like", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagNotLike(String value) {
            addCriterion("article_tag not like", value, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagIn(List<String> values) {
            addCriterion("article_tag in", values, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagNotIn(List<String> values) {
            addCriterion("article_tag not in", values, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagBetween(String value1, String value2) {
            addCriterion("article_tag between", value1, value2, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleTagNotBetween(String value1, String value2) {
            addCriterion("article_tag not between", value1, value2, "articleTag");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdIsNull() {
            addCriterion("article_author_id is null");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdIsNotNull() {
            addCriterion("article_author_id is not null");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdEqualTo(Integer value) {
            addCriterion("article_author_id =", value, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdNotEqualTo(Integer value) {
            addCriterion("article_author_id <>", value, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdGreaterThan(Integer value) {
            addCriterion("article_author_id >", value, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("article_author_id >=", value, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdLessThan(Integer value) {
            addCriterion("article_author_id <", value, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdLessThanOrEqualTo(Integer value) {
            addCriterion("article_author_id <=", value, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdIn(List<Integer> values) {
            addCriterion("article_author_id in", values, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdNotIn(List<Integer> values) {
            addCriterion("article_author_id not in", values, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdBetween(Integer value1, Integer value2) {
            addCriterion("article_author_id between", value1, value2, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleAuthorIdNotBetween(Integer value1, Integer value2) {
            addCriterion("article_author_id not between", value1, value2, "articleAuthorId");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleIsNull() {
            addCriterion("article_image_title is null");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleIsNotNull() {
            addCriterion("article_image_title is not null");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleEqualTo(String value) {
            addCriterion("article_image_title =", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleNotEqualTo(String value) {
            addCriterion("article_image_title <>", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleGreaterThan(String value) {
            addCriterion("article_image_title >", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleGreaterThanOrEqualTo(String value) {
            addCriterion("article_image_title >=", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleLessThan(String value) {
            addCriterion("article_image_title <", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleLessThanOrEqualTo(String value) {
            addCriterion("article_image_title <=", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleLike(String value) {
            addCriterion("article_image_title like", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleNotLike(String value) {
            addCriterion("article_image_title not like", value, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleIn(List<String> values) {
            addCriterion("article_image_title in", values, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleNotIn(List<String> values) {
            addCriterion("article_image_title not in", values, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleBetween(String value1, String value2) {
            addCriterion("article_image_title between", value1, value2, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticleImageTitleNotBetween(String value1, String value2) {
            addCriterion("article_image_title not between", value1, value2, "articleImageTitle");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsIsNull() {
            addCriterion("article_pageviews is null");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsIsNotNull() {
            addCriterion("article_pageviews is not null");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsEqualTo(Integer value) {
            addCriterion("article_pageviews =", value, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsNotEqualTo(Integer value) {
            addCriterion("article_pageviews <>", value, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsGreaterThan(Integer value) {
            addCriterion("article_pageviews >", value, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsGreaterThanOrEqualTo(Integer value) {
            addCriterion("article_pageviews >=", value, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsLessThan(Integer value) {
            addCriterion("article_pageviews <", value, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsLessThanOrEqualTo(Integer value) {
            addCriterion("article_pageviews <=", value, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsIn(List<Integer> values) {
            addCriterion("article_pageviews in", values, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsNotIn(List<Integer> values) {
            addCriterion("article_pageviews not in", values, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsBetween(Integer value1, Integer value2) {
            addCriterion("article_pageviews between", value1, value2, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticlePageviewsNotBetween(Integer value1, Integer value2) {
            addCriterion("article_pageviews not between", value1, value2, "articlePageviews");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountIsNull() {
            addCriterion("article_likecount is null");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountIsNotNull() {
            addCriterion("article_likecount is not null");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountEqualTo(Integer value) {
            addCriterion("article_likecount =", value, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountNotEqualTo(Integer value) {
            addCriterion("article_likecount <>", value, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountGreaterThan(Integer value) {
            addCriterion("article_likecount >", value, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountGreaterThanOrEqualTo(Integer value) {
            addCriterion("article_likecount >=", value, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountLessThan(Integer value) {
            addCriterion("article_likecount <", value, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountLessThanOrEqualTo(Integer value) {
            addCriterion("article_likecount <=", value, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountIn(List<Integer> values) {
            addCriterion("article_likecount in", values, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountNotIn(List<Integer> values) {
            addCriterion("article_likecount not in", values, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountBetween(Integer value1, Integer value2) {
            addCriterion("article_likecount between", value1, value2, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleLikecountNotBetween(Integer value1, Integer value2) {
            addCriterion("article_likecount not between", value1, value2, "articleLikecount");
            return (Criteria) this;
        }

        public Criteria andArticleCommentIsNull() {
            addCriterion("article_comment is null");
            return (Criteria) this;
        }

        public Criteria andArticleCommentIsNotNull() {
            addCriterion("article_comment is not null");
            return (Criteria) this;
        }

        public Criteria andArticleCommentEqualTo(Integer value) {
            addCriterion("article_comment =", value, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentNotEqualTo(Integer value) {
            addCriterion("article_comment <>", value, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentGreaterThan(Integer value) {
            addCriterion("article_comment >", value, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentGreaterThanOrEqualTo(Integer value) {
            addCriterion("article_comment >=", value, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentLessThan(Integer value) {
            addCriterion("article_comment <", value, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentLessThanOrEqualTo(Integer value) {
            addCriterion("article_comment <=", value, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentIn(List<Integer> values) {
            addCriterion("article_comment in", values, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentNotIn(List<Integer> values) {
            addCriterion("article_comment not in", values, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentBetween(Integer value1, Integer value2) {
            addCriterion("article_comment between", value1, value2, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleCommentNotBetween(Integer value1, Integer value2) {
            addCriterion("article_comment not between", value1, value2, "articleComment");
            return (Criteria) this;
        }

        public Criteria andArticleHeatIsNull() {
            addCriterion("article_heat is null");
            return (Criteria) this;
        }

        public Criteria andArticleHeatIsNotNull() {
            addCriterion("article_heat is not null");
            return (Criteria) this;
        }

        public Criteria andArticleHeatEqualTo(Integer value) {
            addCriterion("article_heat =", value, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatNotEqualTo(Integer value) {
            addCriterion("article_heat <>", value, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatGreaterThan(Integer value) {
            addCriterion("article_heat >", value, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatGreaterThanOrEqualTo(Integer value) {
            addCriterion("article_heat >=", value, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatLessThan(Integer value) {
            addCriterion("article_heat <", value, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatLessThanOrEqualTo(Integer value) {
            addCriterion("article_heat <=", value, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatIn(List<Integer> values) {
            addCriterion("article_heat in", values, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatNotIn(List<Integer> values) {
            addCriterion("article_heat not in", values, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatBetween(Integer value1, Integer value2) {
            addCriterion("article_heat between", value1, value2, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andArticleHeatNotBetween(Integer value1, Integer value2) {
            addCriterion("article_heat not between", value1, value2, "articleHeat");
            return (Criteria) this;
        }

        public Criteria andCreatedIsNull() {
            addCriterion("created is null");
            return (Criteria) this;
        }

        public Criteria andCreatedIsNotNull() {
            addCriterion("created is not null");
            return (Criteria) this;
        }

        public Criteria andCreatedEqualTo(Date value) {
            addCriterion("created =", value, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedNotEqualTo(Date value) {
            addCriterion("created <>", value, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedGreaterThan(Date value) {
            addCriterion("created >", value, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedGreaterThanOrEqualTo(Date value) {
            addCriterion("created >=", value, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedLessThan(Date value) {
            addCriterion("created <", value, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedLessThanOrEqualTo(Date value) {
            addCriterion("created <=", value, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedIn(List<Date> values) {
            addCriterion("created in", values, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedNotIn(List<Date> values) {
            addCriterion("created not in", values, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedBetween(Date value1, Date value2) {
            addCriterion("created between", value1, value2, "created");
            return (Criteria) this;
        }

        public Criteria andCreatedNotBetween(Date value1, Date value2) {
            addCriterion("created not between", value1, value2, "created");
            return (Criteria) this;
        }

        public Criteria andUpdatedIsNull() {
            addCriterion("updated is null");
            return (Criteria) this;
        }

        public Criteria andUpdatedIsNotNull() {
            addCriterion("updated is not null");
            return (Criteria) this;
        }

        public Criteria andUpdatedEqualTo(Date value) {
            addCriterion("updated =", value, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedNotEqualTo(Date value) {
            addCriterion("updated <>", value, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedGreaterThan(Date value) {
            addCriterion("updated >", value, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedGreaterThanOrEqualTo(Date value) {
            addCriterion("updated >=", value, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedLessThan(Date value) {
            addCriterion("updated <", value, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedLessThanOrEqualTo(Date value) {
            addCriterion("updated <=", value, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedIn(List<Date> values) {
            addCriterion("updated in", values, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedNotIn(List<Date> values) {
            addCriterion("updated not in", values, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedBetween(Date value1, Date value2) {
            addCriterion("updated between", value1, value2, "updated");
            return (Criteria) this;
        }

        public Criteria andUpdatedNotBetween(Date value1, Date value2) {
            addCriterion("updated not between", value1, value2, "updated");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}