package com.myblog.mapper;

import com.myblog.pojo.TbUser;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TbUserMapperTest {
    @Test
    void userMappingReadsAndPersistsTheRoleWithoutARealDatabase() {
        Configuration configuration = new Configuration();
        configuration.addMapper(TbUserMapper.class);
        assertTrue(configuration.getResultMap("com.myblog.mapper.TbUserMapper.BaseResultMap")
                .getResultMappings().stream().anyMatch(mapping -> "role".equals(mapping.getProperty())));
        TbUser user = new TbUser();
        user.setId(7L);
        user.setRole(TbUser.ROLE_USER);
        String insert = configuration.getMappedStatement("com.myblog.mapper.TbUserMapper.insert").getBoundSql(user).getSql();
        String update = configuration.getMappedStatement("com.myblog.mapper.TbUserMapper.updateByPrimaryKeySelective")
                .getBoundSql(user).getSql();
        assertTrue(insert.contains("role"));
        assertTrue(update.contains("role ="));
        assertFalse(update.contains("password ="));
    }
}
