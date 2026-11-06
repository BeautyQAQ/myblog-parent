package com.myblog.mapper;

import com.myblog.pojo.TbArticle;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Locale;

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
