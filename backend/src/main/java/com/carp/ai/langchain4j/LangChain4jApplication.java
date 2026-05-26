package com.carp.ai.langchain4j;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.carp.ai.langchain4j.mapper")
public class LangChain4jApplication {
    public static void main(String[] args) {
        SpringApplication.run(LangChain4jApplication.class, args);
    }
}
