package com.movieticket.movie.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 电影分类实体，映射 db_movie 库的 category 表。
 */
@Data
@TableName("category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;            // 分类名称（动作/喜剧/科幻...）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}