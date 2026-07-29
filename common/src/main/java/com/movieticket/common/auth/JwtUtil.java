package com.movieticket.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具类 纯静态方法，提供生成 JWT 和解析 JWT 的功能
 *
 */
public class JwtUtil {

    public static String generateToken(Long userId, String username,String secret){
        //根据算法 获得响应的密钥
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        //创建JwtBuilder对象 根据传入的信息
        return Jwts.builder()
                .subject(String.valueOf(userId))    // sub 字段 = userId（字符串）
                .claim("username", username)        // 自定义字段，后续 parse 可取出
                .issuedAt(new Date())               // iat 签发时间
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)) // exp 过期时间（7天）
                .signWith(key)                      // 用 HMAC-SHA 签名
                .compact();                         // 生成最终的 JWT 字符串
    }

    //验证 并且实现对 Token的解析
    public static Claims parseToken(String token, String secret){
        //解析并且验证token
        return Jwts.parser()
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //Claims 获取 user id - 为什么这样写 保持其他功能的存粹 可以直接从解析出来的claims获得数据
    public String getUserIdFromClaims(Claims claims){
        return claims.getSubject();
    }

    //同理 获得username
    public String getUsernameFromClaims(Claims claims){
        return claims.get("username", String.class);
    }
}
