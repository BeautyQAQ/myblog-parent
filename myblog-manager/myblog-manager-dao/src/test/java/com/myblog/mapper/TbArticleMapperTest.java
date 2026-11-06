package com.myblog.mapper;

import com.myblog.pojo.TbArticle;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class TbArticleMapperTest {
    private final Configuration configuration = configuration();

    @Test
    void publicQueriesRequirePublishedArticlesAndBindTheRequestedId() {
        for (String query : new String[]{"selectAllWithTextOrderByNew", "selectAllWithTextOrderByHeat",
                "selectPublishedByPrimaryKey"}) {
            String sql = sql(query, 12);
            assertTrue(sql.contains("article_status = 'PUBLISHED'"), query + ": " + sql);
            assertTrue(sql.contains("article_text"), query);
        }
        var detail = statement("selectPublishedByPrimaryKey").getBoundSql(12);
        assertTrue(detail.getSql().contains("article_id = ?"));
        assertEquals("articleId", detail.getParameterMappings().getFirst().getProperty());
    }

    @Test
    void managementQueriesIncludeDraftsOfflineAndLifecycleColumns() {
        for (String query : new String[]{"selectAllOrderByNew", "selectAllOrderByHeat", "selectByPrimaryKey"}) {
            String sql = sql(query, 12);
            assertFalse(sql.contains("'PUBLISHED'"), query);
            assertTrue(sql.contains("article_status"), query);
            assertTrue(sql.contains("published_at"), query);
        }
        var mappings = statement("selectByPrimaryKey").getResultMaps().getFirst().getResultMappings();
        assertTrue(mappings.stream().anyMatch(mapping -> mapping.getProperty().equals("articleStatus")));
        assertTrue(mappings.stream().anyMatch(mapping -> mapping.getProperty().equals("publishedAt")));
    }

    @Test
    void selectiveInsertReturnsGeneratedIdsAndPersistsPublicationFields() {
        TbArticle article = new TbArticle();
        article.setArticleTitle("新文章");
        article.setArticleStatus("PUBLISHED");
        article.setPublishedAt(new Date());
        String sql = sql("insertSelective", article);
        assertTrue(sql.contains("article_status"));
        assertTrue(sql.contains("published_at"));
        assertInstanceOf(Jdbc3KeyGenerator.class, statement("insertSelective").getKeyGenerator());
        assertArrayEquals(new String[]{"articleId"}, statement("insertSelective").getKeyProperties());
    }

    @Test
    void anEditorialPatchDoesNotWriteAuthorCountersOrCreationTimeAndPreservesFirstPublication() {
        TbArticle changes = new TbArticle();
        changes.setArticleId(12);
        changes.setArticleTitle("修改");
        changes.setArticleStatus("PUBLISHED");
        changes.setPublishedAt(new Date());
        String sql = sql("updateByPrimaryKeySelective", changes);
        assertFalse(sql.contains("article_author_id"));
        assertFalse(sql.contains("article_pageviews"));
        assertFalse(sql.contains("article_likecount"));
        assertFalse(sql.contains("article_comment"));
        assertFalse(sql.contains("article_heat"));
        assertFalse(sql.contains("created"));
        assertTrue(sql.toLowerCase(Locale.ROOT).contains("published_at = coalesce(published_at, ?)"));
    }

    @Test
    void searchBindsEscapedTitleAndAnExactTagWithoutInterpolatingSql() throws Exception {
        String pattern = "%O'Reilly =% =_ ==%";
        String tag = "Java' OR 1=1 --";
        Map<String, Object> parameters = Map.of("titlePattern", pattern, "tag", tag, "orderBy", "new");
        var statement = statement("selectPublishedArticles");
        var boundSql = statement.getBoundSql(parameters);
        String sql = sql("selectPublishedArticles", parameters);
        assertTrue(sql.contains("article_status = 'PUBLISHED'"));
        assertTrue(sql.contains("article_title LIKE ? ESCAPE '='"));
        assertTrue(sql.contains("TRIM(article_tag) = ?"));
        assertFalse(sql.contains(pattern));
        assertFalse(sql.contains(tag));
        assertEquals(java.util.List.of("titlePattern", "tag"), boundSql.getParameterMappings().stream()
                .map(org.apache.ibatis.mapping.ParameterMapping::getProperty).toList());

        Map<Integer, String> boundValues = new HashMap<>();
        PreparedStatement jdbc = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("setString")) {
                        boundValues.put((Integer) args[0], (String) args[1]);
                    }
                    return null;
                });
        new DefaultParameterHandler(statement, parameters, boundSql).setParameters(jdbc);
        assertEquals(Map.of(1, pattern, 2, tag), boundValues);
    }

    @Test
    void absentSearchFiltersKeepOnlyThePublicationConstraint() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("titlePattern", null);
        parameters.put("tag", null);
        parameters.put("orderBy", null);
        for (String empty : new String[]{null, ""}) {
            parameters.put("titlePattern", empty);
            parameters.put("tag", empty);
            String sql = sql("selectPublishedArticles", parameters);
            assertTrue(sql.contains("where article_status = 'PUBLISHED'"));
            assertFalse(sql.contains(" LIKE "));
            assertFalse(sql.contains("TRIM(article_tag) ="));
            assertTrue(statement("selectPublishedArticles").getBoundSql(parameters).getParameterMappings().isEmpty());
            assertTrue(sql.endsWith("order by published_at desc, article_id desc"));
        }
    }

    @Test
    void onlyTheFixedHeatBranchCanChangeTheStablePublicationOrdering() {
        String heat = sql("selectPublishedArticles", Map.of("titlePattern", "", "tag", "", "orderBy", "heat"));
        assertTrue(heat.endsWith("order by article_heat desc, published_at desc, article_id desc"));
        for (String order : new String[]{"new", "article_id; DROP TABLE tb_article", "heat desc --"}) {
            String sql = sql("selectPublishedArticles", Map.of("titlePattern", "", "tag", "", "orderBy", order));
            assertTrue(sql.endsWith("order by published_at desc, article_id desc"), order);
            assertFalse(sql.contains("DROP"));
            assertFalse(sql.contains("--"));
        }
    }

    @Test
    void titleAndTagCanEachFilterIndependently() {
        String titleOnly = sql("selectPublishedArticles",
                Map.of("titlePattern", "%Java%", "tag", "", "orderBy", "new"));
        assertTrue(titleOnly.contains("article_title LIKE ? ESCAPE '='"));
        assertFalse(titleOnly.contains("TRIM(article_tag) ="));
        String tagOnly = sql("selectPublishedArticles",
                Map.of("titlePattern", "", "tag", "Java", "orderBy", "new"));
        assertFalse(tagOnly.contains(" LIKE "));
        assertTrue(tagOnly.contains("TRIM(article_tag) = ?"));
    }

    @Test
    void tagChoicesOnlyExposeDistinctNonemptyPublishedTagsInStableOrder() {
        String sql = sql("selectPublishedTags", null);
        assertTrue(sql.contains("select distinct TRIM(article_tag) as tag"));
        assertTrue(sql.contains("where article_status = 'PUBLISHED'"));
        assertTrue(sql.contains("article_tag is not null"));
        assertTrue(sql.contains("TRIM(article_tag) != ''"));
        assertTrue(sql.endsWith("order by tag asc"));
        assertEquals(String.class, statement("selectPublishedTags").getResultMaps().getFirst().getType());
    }

    private String sql(String name, Object argument) {
        return statement(name).getBoundSql(argument).getSql().replaceAll("\\s+", " ").strip();
    }

    private org.apache.ibatis.mapping.MappedStatement statement(String name) {
        return configuration.getMappedStatement(TbArticleMapper.class.getName() + "." + name);
    }

    private static Configuration configuration() {
        Configuration configuration = new Configuration();
        configuration.addMapper(TbArticleMapper.class);
        return configuration;
    }
}
