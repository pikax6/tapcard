package com.zxy.tapcard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 一卡通结算系统启动入口。
 * <p>
 * MapperScan 指到 dao 包，Dao 接口不用逐个加 @Mapper。
 */
@SpringBootApplication
@MapperScan("com.zxy.tapcard.dao")
public class TapcardApp {

    public static void main(String[] args) {
        SpringApplication.run(TapcardApp.class, args);
    }
}
