package com.movieticket.movie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.movieticket.common.result.Result;
import com.movieticket.movie.entity.Category;
import com.movieticket.movie.entity.Movie;
import com.movieticket.movie.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/movie")
public class MovieController {

    private final MovieService movieService;


    @GetMapping("/list")
    public Result<Page<Movie>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) int status

    ){
        Page<Movie> listpage = movieService.listpage(page, size, categoryId, status);
        return Result.success(listpage);
    }


    @GetMapping("/{id}")
    //PathVariable Long id
    public Result<Movie> getById(@PathVariable Long id){
        Movie movie = movieService.getById(id);
        return Result.success(movie);
    }

    @GetMapping("/category/List")
    public Result<List<Category>> getCategoryList(){
        List<Category> list = movieService.getCategoryList();
        return Result.success(list);
    }

}
