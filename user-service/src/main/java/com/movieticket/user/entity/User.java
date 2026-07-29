package com.movieticket.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

@Data
@TableName("user")//指定映射的表名,可以自动根据实体类完成下划线,驼峰的转化
@AllArgsConstructor
@NoArgsConstructor
public class User {
    //指定逐渐 数据库中自增 不需要在手动设id
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String password;
    private String nickname;
    private String phone;
    private String  email;
    private Integer status;


    //在创建时自动进行填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    //在更新时自动进行填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
