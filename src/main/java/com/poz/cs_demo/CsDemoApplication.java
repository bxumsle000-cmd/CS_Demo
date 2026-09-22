package com.poz.cs_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// 掃描並註冊所有 @ConfigurationProperties 類別（例如 security/JwtProperties）
@ConfigurationPropertiesScan
public class CsDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsDemoApplication.class, args);
    }

}
