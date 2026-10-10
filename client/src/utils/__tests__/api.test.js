import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('API utilities', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should handle successful GET request', async () => {
    globalThis.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ data: 'test' }),
      })
    );

    const response = await fetch('/api/test');
    const data = await response.json();
    
    expect(response.ok).toBe(true);
    expect(data).toEqual({ data: 'test' });
  });

  it('should handle failed POST request', async () => {
    globalThis.fetch = vi.fn(() =>
      Promise.resolve({
        ok: false,
        status: 400,
        json: () => Promise.resolve({ message: 'Bad request' }),
      })
    );

    const response = await fetch('/api/test', {
      method: 'POST',
      body: JSON.stringify({ test: 'data' }),
    });
    
    expect(response.ok).toBe(false);
    expect(response.status).toBe(400);
  });

  it('should handle network errors', async () => {
    globalThis.fetch = vi.fn(() => Promise.reject(new Error('Network error')));

    await expect(fetch('/api/test')).rejects.toThrow('Network error');
  });
});
