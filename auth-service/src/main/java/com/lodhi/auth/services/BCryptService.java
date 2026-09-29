package com.lodhi.auth.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lodhi.auth.exceptions.AuthenticationOverloadedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BCryptService {

    private final PasswordEncoder passwordEncoder;
    private final ExecutorService bcryptExecutor;
    private final Semaphore bcryptSemaphore;

    public BCryptService(
            PasswordEncoder passwordEncoder,
            @Qualifier("bcryptExecutor") ExecutorService bcryptExecutor,
            @Qualifier("bcryptSemaphore") Semaphore bcryptSemaphore) {
        this.passwordEncoder = passwordEncoder;
        this.bcryptExecutor = bcryptExecutor;
        this.bcryptSemaphore = bcryptSemaphore;
    }

    /**
     * Performs BCrypt password matching with concurrency control.
     * 
     * Uses Virtual Threads + Semaphore approach:
     * 1. Acquires permit (waits up to 30 seconds)
     * 2. Submits BCrypt work to virtual thread executor
     * 3. Releases permit when done
     * 
     * This prevents:
     * - CPU overload (semaphore limits concurrent BCrypt operations)
     * - Thread pool exhaustion (virtual threads are lightweight)
     * - Rejected executions (waits for permit instead of rejecting)
     * 
     * @param rawPassword Plain text password
     * @param encodedPassword BCrypt hashed password
     * @return true if passwords match
     * @throws AuthenticationOverloadedException if can't acquire permit within timeout
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        boolean permitAcquired = false;
        
        try {
            // Try to acquire permit (wait up to 30 seconds)
            permitAcquired = bcryptSemaphore.tryAcquire(30, TimeUnit.SECONDS);
            
            if (!permitAcquired) {
                log.warn("BCrypt semaphore timeout - system overloaded");
                throw new AuthenticationOverloadedException(
                    "Authentication service is temporarily overloaded. Please try again."
                );
            }

            // Submit to virtual thread executor
            Future<Boolean> future = bcryptExecutor.submit(
                () -> passwordEncoder.matches(rawPassword, encodedPassword)
            );

            // Wait for result (virtual thread, non-blocking on OS level)
            return future.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("BCrypt operation interrupted", e);
            throw new RuntimeException("Password check interrupted", e);
        } catch (Exception e) {
            log.error("BCrypt execution failed", e);
            throw new RuntimeException("Password check failed", e);
        } finally {
            // Always release permit
            if (permitAcquired) {
                bcryptSemaphore.release();
            }
        }
    }
    
    /**
     * Get current BCrypt queue status for monitoring
     */
    public int getAvailablePermits() {
        return bcryptSemaphore.availablePermits();
    }
    
    /**
     * Get queue length (approximate)
     */
    public int getQueueLength() {
        return bcryptSemaphore.getQueueLength();
    }
}
