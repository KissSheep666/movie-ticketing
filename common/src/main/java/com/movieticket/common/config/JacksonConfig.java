package com.movieticket.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 对Jackson 进行序列化配置 统一输出的JSON格式
 * 不配这个的话LocalDateTime 会序列化成时间戳
 */

/**
 * Jackson序列化配置
 */
@Configuration
//在最开始(容器初始化的时候)执行
public class JacksonConfig {

    //此时将返回的数据放入容器进行管理 同时如果有多个同样类型的返回值的时候 可以起别名 同时在注入的时候指定名称
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        //注册序列化器 指定输出的JSON格式
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(module);
        return objectMapper;
    }
    

//nacos 配置 对每个模块 指定配置 比如对于common 和 user-service 指定相应的数据库端口和数据库名 (等于代替了 独立模块中的yml配置)

}
