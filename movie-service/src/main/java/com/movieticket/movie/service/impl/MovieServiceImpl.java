package com.movieticket.movie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.movie.entity.Category;
import com.movieticket.movie.entity.Movie;
import com.movieticket.movie.mapper.CategoryMapper;
import com.movieticket.movie.mapper.MovieMapper;
import com.movieticket.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MovieServiceImpl implements MovieService {

    private final MovieMapper movieMapper;

    private final CategoryMapper categoryMapper;

    /**
     * 分页查询 根据参数 返回数据
     * @param page
     * @param size
     * @param categoryId
     * @param status
     * @return
     */
    @Override
    public Page<Movie> listpage(int page, int size, Long categoryId, int status) {
        Page<Movie> pageParam = new Page<>(page, size);//当前页数 每页的数据
        //使用动态拼接[拼接参数
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null){
            wrapper.eq(Movie::getCategoryId, categoryId);
        }
        if (status != -1){
            wrapper.eq(Movie::getStatus, status);
        }
        //参数 降序排序 根据日趋 进行降序派略
        wrapper.orderByDesc(Movie::getCreateTime);

        //mp 传入构造器 和 参数 直接会进行分页查询 会自动返回内容

        return movieMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 根据id 查看详情
     * @param id
     * @return
     */
    @Override
    public Movie getById(Long id) {
        Movie movie = movieMapper.selectById(id);
        if (movie == null){
            throw new BizException(BizCodeEnum.MOVIE_NOT_FOUND);
        }
        return movie;
    }

    /**
     * 获取完整的分类列表
     * @return
     */

    @Override
    public List<Category> getCategoryList() {
        return categoryMapper.selectList(null);
    }
}
