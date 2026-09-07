package com.unicauca.piedrazul;

import org.springframework.boot.SpringApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;

@Modulith
@EnableScheduling
public class PiedrazulApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiedrazulApplication.class, args);
    }

}