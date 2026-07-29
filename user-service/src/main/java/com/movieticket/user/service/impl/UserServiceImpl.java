package com.movieticket.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.movieticket.common.auth.JwtUtil;
import com.movieticket.common.exception.BizCodeEnum;
import com.movieticket.common.exception.BizException;
import com.movieticket.user.entity.User;
import com.movieticket.user.mapper.UserMapper;
import com.movieticket.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.Digest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.sql.Wrapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserMapper userMapper;

    /**
     * 用户注册
     * 实现逻辑 输入账号密码 创建用户
     * 密码储存过程使用md5加密
     * @param name
     * @param password
     * @param nickname
     * @return
     */
    @Override
    public User register(String name, String password, String nickname) {
        //确定用户名是否已存在
        LambdaQueryWrapper< User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getName, name);
        User oldUser = userMapper.selectOne(queryWrapper);
        if (oldUser != null){
            log.info("用户名已存在");
            throw new BizException(BizCodeEnum.PARAM_ERROR);
        }
        User user = new User();

        //进行用户的创建 对密码进行md5加密
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());
        user.setPassword(md5Password);

        //对其余数据进行填充
        // 3. 入库：INSERT INTO user (username, password, ...) VALUES (...)
        user.setName(name);
        user.setNickname(nickname != null ? nickname : name); // 没传昵称默认用用户名
        user.setStatus(1);  // 1=正常，0=禁用
        userMapper.insert(user);  // MyBatis-Plus 自动回填自增 ID 到 user.id

        log.info("用户注册成功: username={}, id={}", name, user.getId());
        return user;
    }

    /**
     * 用户登录 查库进行校验
     * @param name
     * @param password
     * @return
     */
    @Override
    public String login(String name, String password) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getName, name);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null){
            log.info("用户不存在");
            throw new BizException(BizCodeEnum.PASSWORD_ERROR);
        }
        if (user.getStatus() == 0){
            log.info("用户被禁用");
            throw new BizException(BizCodeEnum.USER_DISABLED);
        }
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (user.getPassword().equals(md5Password)){
            log.info("用户登录成功: username={}", name);
            //登录成功 返回生成的token
            return JwtUtil.generateToken(user.getId(), user.getName(), "secret");
        }
        //此时密码错误 抛出异常
        throw new BizException(BizCodeEnum.PASSWORD_ERROR);
    }


    /**
     *
     * @param userid
     * @return
     */
    @Override
    public User getUserById(Long userid) {
        User user = userMapper.selectById(userid);
        user.setPassword(null);
        return user;
    }
}