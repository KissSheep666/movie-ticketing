package com.movieticket.user.controller;

import com.movieticket.common.result.Result;
import com.movieticket.user.entity.User;
import com.movieticket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Request;
import org.springframework.web.bind.annotation.*;

/**
 * 需要实现的功能
 * POST register 用户登录
 * POST login 用户登录
 * GET current 获取当前用户的信息
 *
 */

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    /**
     * 登录功能 对 返回的密码进行校验
     * @param
     * @return
     */

    @PostMapping("/register")
    public Result<User> register(
            @RequestParam  String name,
            @RequestParam String password,
            @RequestParam(required = false) String nickname
            ){
        User register = userService.register(name, password, nickname);
        register.setPassword(null);
        return Result.success(register);
    }


    /**
     * 登录功能 实现逻辑 输入账号密码 返回一个token 用于后续的登录 以及校验
     * 登录功能 不用重复校验 别的模块如果访问 是 统一进行拦截 使用token进行过滤
     * @param name
     * @param password
     * @return
     */
    @PostMapping("/login")
    public Result<String> login(
            @RequestParam String name,
            @RequestParam String password

    ){
        userService.login(name, password);
        return Result.success("登录成功");
    }


    /**
     * 获取当前用户的具体信息
     * @param userid
     * @return
     */
    @GetMapping("/current")
    public Result<User> current(
            //利用反射的思想 从请求头中获取用户id
            @RequestHeader("X-User-Id")  Long userid
    ){
        User user = userService.getUserById(userid);
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        user.setPassword(null);
        return Result.success(user);
    }





}