package com.draftly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
@EnableRetry   // Requirement 6: Enables automated retry logic for failed sends
public class DraftlyApplication {
    public static void main(String[] args) {
        SpringApplication.run(DraftlyApplication.class, args);
    }
}