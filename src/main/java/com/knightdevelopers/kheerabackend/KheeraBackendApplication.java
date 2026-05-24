package com.knightdevelopers.kheerabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KheeraBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KheeraBackendApplication.class, args);
    }



}
