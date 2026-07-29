package com.movieticket.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.movieticket.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 继承BaseMapper<User> 自动获得CRUD 方法
 * 主键查询 wrapper 条件查询(多条,单条) 插入 更新 删除等基础功能 如果需要拓展或修改 可以重写
 */

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

