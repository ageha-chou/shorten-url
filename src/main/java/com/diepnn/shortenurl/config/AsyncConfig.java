package com.diepnn.shortenurl.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
    /**
     * Primary async executor for the entire application
     * Used by default when no specific executor is specified
     */
    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);           // Increased for app-wide usage
        executor.setMaxPoolSize(32);           // Higher max for diverse workloads
        executor.setQueueCapacity(2000);       // Larger queue for multiple services
        executor.setThreadNamePrefix("app-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("Async task rejected, executing synchronously as fallback");
            runnable.run();
        });

        executor.initialize();
        return executor;
    }

    /**
     * Specialized executor for URL access operations (high frequency, short duration)
     * Use this for URL redirect analytics that need fast processing
     */
    @Bean(name = "urlAccessExecutor")
    public Executor urlAccessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("url-access-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("URL access task rejected, executing synchronously as fallback");
            runnable.run();
        });

        executor.initialize();
        return executor;
    }

    /**
     * Specialized executor for heavy database operations (lower frequency, longer duration)
     * Use this for complex analytics, reports, or batch operations
     */
    @Bean(name = "databaseExecutor")
    public Executor databaseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);           // Fewer threads for heavy DB ops
        executor.setMaxPoolSize(8);            // Lower max to prevent DB overload
        executor.setQueueCapacity(500);        // Smaller queue for heavy operations
        executor.setThreadNamePrefix("db-ops-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120); // Longer wait for DB operations

        executor.setRejectedExecutionHandler((runnable, executor1) -> {
            log.warn("Database operation rejected, executing synchronously as fallback");
            runnable.run();
        });

        executor.initialize();
        return executor;
    }
}
