package com.peterscode.ecommerce_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class EcommerceManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceManagementSystemApplication.class, args);
    }

}
