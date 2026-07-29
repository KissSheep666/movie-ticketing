package com.movieticket.movie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.movie.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}