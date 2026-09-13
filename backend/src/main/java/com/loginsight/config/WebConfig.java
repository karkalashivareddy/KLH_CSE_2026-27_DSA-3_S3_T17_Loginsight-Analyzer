package com.loginsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.loginsight.analytics.FrequencyAnalyzer;

/**
 * Infrastructure configuration for the backend. Phase 1 keeps this to a minimum; web/CORS
 * configuration joins this class in Phase 10 when the REST API is wired.
 *
 * <p>The shared task executor is the seeding point for the Phase 8 parallel layer, which may
 * reuse this pool (docs/02 §11). The {@link FrequencyAnalyzer} is registered here so the class
 * itself stays free of Spring annotations.</p>
 */
@Configuration
public class WebConfig {

    @Bean(name = "sharedTaskExecutor")
    public ThreadPoolTaskExecutor sharedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors());
        executor.setQueueCapacity(512);
        executor.setThreadNamePrefix("loginsight-");
        executor.initialize();
        return executor;
    }

    @Bean
    public FrequencyAnalyzer frequencyAnalyzer() {
        return new FrequencyAnalyzer();
    }
}