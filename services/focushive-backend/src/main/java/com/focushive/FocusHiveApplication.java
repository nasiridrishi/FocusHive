package com.focushive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableJpaAuditing  // Disabled for minimal startup
@EnableFeignClients
@EnableScheduling
public class FocusHiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(FocusHiveApplication.class, args);
    }
}