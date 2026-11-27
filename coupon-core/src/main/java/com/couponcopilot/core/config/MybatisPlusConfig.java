package com.couponcopilot.core.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus Mapper 扫描配置。
 * 独立于启动类放置，避免 @WebMvcTest 切片测试因找不到 SqlSessionFactory 而启动失败。
 */
@Configuration
@MapperScan("com.couponcopilot.core.dao.mapper")
public class MybatisPlusConfig {
}
