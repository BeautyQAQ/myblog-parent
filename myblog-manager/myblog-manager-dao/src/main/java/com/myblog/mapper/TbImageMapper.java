package com.myblog.mapper;

import com.myblog.pojo.TbImage;
import org.apache.ibatis.annotations.Param;

public interface TbImageMapper {
    int insert(TbImage image);

    TbImage selectByPrimaryKey(@Param("id") String id);
}
