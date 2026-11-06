package com.myblog.mapper;

import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TbImageMapperTest {
    @Test
    void loadsThePackagedImageMapperXmlWithoutADatabase() {
        Configuration configuration = new Configuration();
        configuration.addMapper(TbImageMapper.class);

        assertTrue(configuration.hasStatement("com.myblog.mapper.TbImageMapper.insert"));
        assertTrue(configuration.hasStatement("com.myblog.mapper.TbImageMapper.selectByPrimaryKey"));
    }
}
