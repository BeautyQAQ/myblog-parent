package com.myblog.mapper;

import com.myblog.pojo.TbUserlog;
import com.myblog.pojo.TbUserlogExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TbUserlogMapper {
    int countByExample(TbUserlogExample example);

    int deleteByExample(TbUserlogExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(TbUserlog record);

    int insertSelective(TbUserlog record);

    List<TbUserlog> selectByExample(TbUserlogExample example);

    TbUserlog selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") TbUserlog record, @Param("example") TbUserlogExample example);

    int updateByExample(@Param("record") TbUserlog record, @Param("example") TbUserlogExample example);

    int updateByPrimaryKeySelective(TbUserlog record);

    int updateByPrimaryKey(TbUserlog record);
}