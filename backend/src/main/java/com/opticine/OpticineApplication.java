package com.opticine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpticineApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpticineApplication.class, args);
    }
}
