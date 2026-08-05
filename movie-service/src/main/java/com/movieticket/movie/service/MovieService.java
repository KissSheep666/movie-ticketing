package com.movieticket.movie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movieticket.movie.entity.Category;
import com.movieticket.movie.entity.Movie;
import org.springframework.stereotype.Service;

import java.security.AlgorithmParameterGenerator;
import java.util.List;


public interface MovieService {
    /**
     * 发呢里查询带你应列表 支持分类筛选
     * 传入页码 每页数量  分类参数 状态筛选 返回一个完整page对象
     */
    Page<Movie> listpage(int page, int size, Long categoryId, int status);

    /**
     * 查看电影详情 查不到返回错误
     */
    Movie getById(Long id);


    /**
     * 查看电影详情,往后可以根据劣列表 进行查询
     */
    List<Category> getCategoryList();

}
