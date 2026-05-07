package com.xinzhe.projectmentor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.xinzhe.projectmentor.**.mapper")
@SpringBootApplication
public class ProjectmentorServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectmentorServerApplication.class, args);
    }
}