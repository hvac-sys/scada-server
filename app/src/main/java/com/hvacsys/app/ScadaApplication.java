package com.hvacsys.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.hvacsys")
public class ScadaApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ScadaApplication.class, args);
    }
}
