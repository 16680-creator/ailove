package com.ailovedaily;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 爱在朝夕 - 情侣生活记录小程序 启动类
 */
@SpringBootApplication
@EnableScheduling
public class AiLoveDailyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiLoveDailyApplication.class, args);
    }
}
