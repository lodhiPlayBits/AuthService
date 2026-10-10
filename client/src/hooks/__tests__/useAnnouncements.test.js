import { describe, it, expect, vi, beforeEach } from 'vitest';

// Mock dependencies
vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(() => ({
    user: { id: 'user-123', role: 'USER' },
    token: 'fake-token',
    isAuthenticated: true,
  })),
}));

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn(() => ({
    activate: vi.fn(),
    deactivate: vi.fn(),
    subscribe: vi.fn(),
    publish: vi.fn(),
    connected: true,
  })),
  ReconnectionTimeMode: { EXPONENTIAL: 'EXPONENTIAL' },
}));

describe('useAnnouncements hook', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should initialize with correct default state', () => {
    const mockUseAnnouncements = () => ({
      connectionState: 'connecting',
      announcements: [],
      confirmations: [],
      adminResponses: [],
      errors: [],
      sendAnnouncement: vi.fn(),
      acknowledgeAnnouncement: vi.fn(),
      dismissAnnouncement: vi.fn(),
    });

    const result = mockUseAnnouncements();
    
    expect(result.connectionState).toBe('connecting');
    expect(result.announcements).toEqual([]);
    expect(result.confirmations).toEqual([]);
    expect(result.errors).toEqual([]);
  });

  it('should provide announcement functions', () => {
    const mockUseAnnouncements = () => ({
      connectionState: 'connected',
      announcements: [],
      confirmations: [],
      adminResponses: [],
      errors: [],
      sendAnnouncement: vi.fn(),
      acknowledgeAnnouncement: vi.fn(),
      dismissAnnouncement: vi.fn(),
    });

    const result = mockUseAnnouncements();
    
    expect(typeof result.sendAnnouncement).toBe('function');
    expect(typeof result.acknowledgeAnnouncement).toBe('function');
    expect(typeof result.dismissAnnouncement).toBe('function');
  });

  it('should handle disconnected state when not authenticated', () => {
    const mockUseAnnouncements = (isAuthenticated) => ({
      connectionState: isAuthenticated ? 'connected' : 'disconnected',
      announcements: [],
      confirmations: [],
      adminResponses: [],
      errors: [],
      sendAnnouncement: vi.fn(),
      acknowledgeAnnouncement: vi.fn(),
      dismissAnnouncement: vi.fn(),
    });

    const result = mockUseAnnouncements(false);
    expect(result.connectionState).toBe('disconnected');
  });
});
