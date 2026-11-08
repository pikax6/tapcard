package com.zxy.tapcard.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisCfg {

    @Bean
    public MybatisPlusInterceptor mpInterceptor() {
        MybatisPlusInterceptor it = new MybatisPlusInterceptor();
        it.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        it.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 放最后
        return it;
    }
}
