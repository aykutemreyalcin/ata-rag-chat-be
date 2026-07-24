package com.ata.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtaRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtaRagApplication.class, args);
    }
}
