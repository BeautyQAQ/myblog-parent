package com.myblog.mapper;

import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArticleInteractionMapperTest {
    private final Configuration configuration = configuration();

    @Test
    void locksOnlyPublishedArticlesBeforeReadingOrWritingStatistics() {
        String sql = sql("selectPublishedForUpdate", 12);
        assertTrue(sql.contains("article_id = ? AND article_status = 'PUBLISHED'"));
        assertTrue(sql.endsWith("FOR UPDATE"));
        assertFalse(sql.contains("article_text"));
    }

    @Test
    void recentViewsUseDatabaseTimeAndTheExactArticleVisitorPair() throws Exception {
        String key = "a".repeat(64);
        var parameters = Map.of("articleId", 12, "visitorKey", key);
        String sql = sql("hasRecentView", parameters);
        assertTrue(sql.contains("article_id = ? AND visitor_key = ?"));
        assertTrue(sql.contains("viewed_at > CURRENT_TIMESTAMP(6) - INTERVAL 30 MINUTE"));
        assertEquals(Map.of(1, 12, 2, key), boundValues("hasRecentView", parameters));
        assertTrue(sql("saveView", parameters).contains("ON DUPLICATE KEY UPDATE viewed_at = CURRENT_TIMESTAMP(6)"));
    }

    @Test
    void likesBindArticleAndTrustedUserOnBothInsertionAndDeletion() throws Exception {
        var parameters = Map.of("articleId", 12, "userId", 26L);
        for (String method : List.of("hasLike", "insertLike", "deleteLike")) {
            assertEquals(Map.of(1, 12, 2, 26L), boundValues(method, parameters), method);
        }
        assertTrue(sql("deleteLike", parameters).contains("article_id = ? AND user_id = ?"));
    }

    @Test
    void countersUpdateAtomicallyWithoutChangingEditorialTimestampsOrOtherStatistics() {
        String view = sql("incrementViews", 12);
        assertTrue(view.contains("GREATEST(COALESCE(article_pageviews, 0), 0) + 1"));
        assertTrue(view.contains("5 * GREATEST(article_likecount, 0)"));
        String like = sql("changeLikeCount", Map.of("articleId", 12, "delta", -1));
        assertTrue(like.contains("article_likecount + ?"));
        for (String query : List.of(view, like)) {
            assertTrue(query.contains("WHERE article_id = ? AND article_status = 'PUBLISHED'"));
            assertTrue(query.contains("LEAST(2147483647"));
            assertFalse(query.contains("updated"));
            assertFalse(query.contains("article_comment"));
        }
    }

    private String sql(String name, Object argument) {
        return statement(name).getBoundSql(argument).getSql().replaceAll("\\s+", " ").strip();
    }

    private Map<Integer, Object> boundValues(String name, Object parameters) throws Exception {
        var statement = statement(name);
        Map<Integer, Object> values = new HashMap<>();
        PreparedStatement jdbc = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("set")) values.put((Integer) args[0], args[1]);
                    return null;
                });
        new DefaultParameterHandler(statement, parameters, statement.getBoundSql(parameters)).setParameters(jdbc);
        return values;
    }

    private org.apache.ibatis.mapping.MappedStatement statement(String name) {
        return configuration.getMappedStatement(ArticleInteractionMapper.class.getName() + "." + name);
    }

    private static Configuration configuration() {
        Configuration configuration = new Configuration();
        configuration.addMapper(ArticleInteractionMapper.class);
        return configuration;
    }
}
