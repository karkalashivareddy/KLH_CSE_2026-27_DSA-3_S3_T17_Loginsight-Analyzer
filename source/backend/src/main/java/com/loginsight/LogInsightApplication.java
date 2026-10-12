package com.loginsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the LogInsight Analyzer backend.
 *
 * <p>Phase 1 skeleton: proves the Spring Boot application context boots. Business endpoints,
 * log parsing, and the DSA engine land in later phases.</p>
 */
@SpringBootApplication
public class LogInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogInsightApplication.class, args);
    }
}