package com.movieticket.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 电影实体，映射 db_movie 库的 movie 表。
 */
@Data
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;            // 电影名称
    private Long categoryId;        // 分类ID
    private String director;        // 导演
    private String cast;            // 主演（逗号分隔）
    private Integer duration;       // 时长（分钟）
    private String posterUrl;       // 海报URL
    private String description;     // 简介
    private BigDecimal rating;      // 评分（0.0~10.0）
    private Integer status;         // 1=即将上映，2=热映中，3=已下映

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}