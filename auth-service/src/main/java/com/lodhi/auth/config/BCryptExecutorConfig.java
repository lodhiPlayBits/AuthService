package com.lodhi.auth.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BCryptExecutorConfig {

    /**
     * Virtual Thread Executor for BCrypt password checking.
     * Uses Java 21 Virtual Threads which are lightweight and don't block OS threads.
     * Can handle thousands of concurrent BCrypt operations efficiently.
     */
    @Bean(name = "bcryptExecutor", destroyMethod = "shutdown")
    public ExecutorService bcryptExecutor() {
        // Virtual threads: lightweight, don't block OS threads during BCrypt
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Semaphore to limit concurrent BCrypt operations.
     * Prevents CPU overload by limiting active password checks.
     * 
     * Tuning for t3.large (2 vCPUs):
     * - Optimal: 6 concurrent BCrypt operations (vCPUs × 3)
     * - Each BCrypt takes ~100-300ms CPU time
     * - Virtual threads wait for permit (non-blocking)
     * - CPU-intensive work is bounded
     * - No rejected executions, just queueing
     * 
     * For larger instances, scale up:
     * - 4 vCPUs: 12 concurrent
     * - 8 vCPUs: 24 concurrent
     * - 16 vCPUs: 48 concurrent
     */
    @Bean(name = "bcryptSemaphore")
    public Semaphore bcryptSemaphore(
            @Value("${auth.bcrypt.max-concurrent:6}") int maxConcurrent) {
        return new Semaphore(maxConcurrent, true); // fair=true for FIFO ordering
    }
}
