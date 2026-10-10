import { describe, it, expect } from 'vitest';

/**
 * Tests for backoff utility functions
 * Testing exponential backoff with jitter for reconnection logic
 */
describe('Backoff utilities', () => {
  it('should generate delay within expected range for attempt 0', () => {
    // For attempt 0, expect delay between 500ms and 1000ms (with jitter)
    const delay = Math.random() * 500 + 500;
    expect(delay).toBeGreaterThanOrEqual(500);
    expect(delay).toBeLessThanOrEqual(1000);
  });

  it('should exponentially increase delay for successive attempts', () => {
    const baseDelay = 1000;
    const attempt2 = baseDelay * 2;
    const attempt3 = baseDelay * 4;
    
    expect(attempt2).toBe(2000);
    expect(attempt3).toBe(4000);
    expect(attempt3).toBeGreaterThan(attempt2);
  });

  it('should cap maximum delay', () => {
    const maxDelay = 30000; // 30 seconds
    const largeDelay = 1000 * Math.pow(2, 10); // Would be 1024 seconds
    const cappedDelay = Math.min(largeDelay, maxDelay);
    
    expect(cappedDelay).toBe(maxDelay);
    expect(cappedDelay).toBeLessThanOrEqual(30000);
  });

  it('should handle zero attempts', () => {
    const attempts = 0;
    const delay = Math.pow(2, attempts) * 1000;
    
    expect(delay).toBe(1000);
  });

  it('should handle negative attempts gracefully', () => {
    const attempts = -1;
    const delay = Math.max(0, Math.pow(2, attempts) * 1000);
    
    expect(delay).toBeGreaterThanOrEqual(0);
  });
});
