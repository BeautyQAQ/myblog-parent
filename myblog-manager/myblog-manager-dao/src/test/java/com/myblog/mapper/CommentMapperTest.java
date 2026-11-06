package com.myblog.mapper;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommentMapperTest {
    private final Configuration configuration = configuration();

    @Test void submissionBindsPayloadAndUniquenessNeverOverwritesCommentContentOrStatus() throws Exception {
        String text = "<script>alert('x')</script>\n' OR 1=1 -- 😀";
        Map<String, Object> params = Map.of("articleId", 7, "userId", 26L, "content", text,
                "requestId", "12345678-abcd-abcd-abcd-123456789012");
        var statement = statement("insertPending");
        var bound = statement.getBoundSql(params);
        String sql = sql("insertPending", params);
        assertTrue(sql.endsWith("ON DUPLICATE KEY UPDATE id = id"));
        assertTrue(sql.contains("'PENDING'")); assertFalse(sql.contains(text));
        assertEquals(List.of("articleId", "userId", "content", "requestId"), bound.getParameterMappings().stream()
                .map(m -> m.getProperty()).toList());
        Map<Integer, Object> values = new HashMap<>();
        PreparedStatement jdbc = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("set")) values.put((Integer) args[0], args[1]);
                    return null;
                });
        new DefaultParameterHandler(statement, params, bound).setParameters(jdbc);
        assertEquals(text, values.get(3));
        assertEquals(params.get("requestId"), values.get(4));
    }

    @Test void ownListsHideDeletedRowsAndCountUsesTheSameConditionsAsThePage() {
        Map<String, Object> own = params(7, 26L, null);
        for (String name : new String[]{"selectComments", "countComments"}) {
            String sql = sql(name, own);
            assertTrue(sql.contains("c.article_id = ?"));
            assertTrue(sql.contains("c.user_id = ?"));
            assertTrue(sql.contains("c.status != 'DELETED'"));
        }
        Map<String, Object> publicParams = params(7, null, "APPROVED");
        for (String name : new String[]{"selectComments", "countComments"}) {
            String sql = sql(name, publicParams);
            assertTrue(sql.contains("c.article_id = ?")); assertTrue(sql.contains("c.status = ?"));
            assertFalse(sql.contains("c.user_id = ?"));
        }
        String all = sql("selectComments", params(null, null, null));
        assertFalse(all.contains("WHERE")); assertFalse(all.contains("DELETED"));
    }

    @Test void pagesHaveStableOrderingAndBindLongOffsets() {
        var params = params(7, null, "APPROVED"); params.put("offset", 107374182300L);
        String sql = sql("selectComments", params);
        assertTrue(sql.endsWith("ORDER BY c.created DESC, c.id DESC LIMIT ? OFFSET ?"));
        var mappings = statement("selectComments").getBoundSql(params).getParameterMappings();
        assertEquals("offset", mappings.getLast().getProperty());
        assertEquals(org.apache.ibatis.type.JdbcType.BIGINT, mappings.getLast().getJdbcType());
    }

    @Test void lockingQueriesDoNotAcquireJoinedUserRows() {
        for (String name : new String[]{"lockArticle", "selectForUpdate", "selectRequestForUpdate"}) {
            String sql = sql(name, Map.of("articleId", 7, "id", 42L, "userId", 26L, "requestId", "key"));
            assertTrue(sql.endsWith("FOR UPDATE")); assertFalse(sql.contains("JOIN"));
        }
        assertFalse(sql("selectById", Map.of("id", 42L)).contains("FOR UPDATE"));
    }

    @Test void moderationDoesNotChangeHeatEditorialTimestampsOrOtherCounters() {
        String count = sql("changeApprovedCount", Map.of("articleId", 7, "delta", -1));
        assertTrue(count.contains("article_comment + ?"));
        assertFalse(count.contains("article_heat")); assertFalse(count.contains("updated"));
        assertFalse(count.contains("pageviews")); assertFalse(count.contains("likecount"));
        String status = sql("updateStatus", Map.of("id", 42L, "status", "APPROVED", "moderatedBy", 91L));
        assertTrue(status.contains("moderated_at = CURRENT_TIMESTAMP(6)")); assertTrue(status.contains("WHERE id = ?"));
    }

    private static Map<String, Object> params(Integer articleId, Long userId, String status) {
        Map<String, Object> map = new HashMap<>(); map.put("articleId", articleId); map.put("userId", userId);
        map.put("status", status); map.put("offset", 0L); map.put("limit", 10); return map;
    }
    private String sql(String name, Object params) {
        return statement(name).getBoundSql(params).getSql().replaceAll("\\s+", " ").strip();
    }
    private org.apache.ibatis.mapping.MappedStatement statement(String name) {
        return configuration.getMappedStatement(CommentMapper.class.getName() + "." + name);
    }
    private static Configuration configuration() {
        Configuration c = new Configuration(); c.addMapper(CommentMapper.class); return c;
    }
}
