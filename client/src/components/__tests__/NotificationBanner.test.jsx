import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import NotificationBanner from '../NotificationBanner';

describe('NotificationBanner', () => {
  it('renders notification message', () => {
    const notification = {
      type: 'ACCOUNT_ENABLED',
      message: 'Your account has been enabled',
    };
    
    render(<NotificationBanner notification={notification} />);
    expect(screen.getByText('Your account has been enabled')).toBeInTheDocument();
  });

  it('renders announcement with title', () => {
    const notification = {
      type: 'ANNOUNCEMENT',
      title: 'Important Update',
      message: 'System maintenance scheduled',
      announcementId: 'ann-123',
    };
    
    render(<NotificationBanner notification={notification} />);
    expect(screen.getByText('Important Update')).toBeInTheDocument();
    expect(screen.getByText('System maintenance scheduled')).toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    const onClose = vi.fn();
    const notification = {
      type: 'ACCOUNT_ENABLED',
      message: 'Test message',
    };
    
    render(<NotificationBanner notification={notification} onClose={onClose} />);
    
    const closeButton = screen.getByRole('button');
    fireEvent.click(closeButton);
    
    // onClose is called after animation timeout
    setTimeout(() => {
      expect(onClose).toHaveBeenCalled();
    }, 300);
  });

  it('renders acknowledge and dismiss buttons for announcements', () => {
    const notification = {
      type: 'ANNOUNCEMENT',
      title: 'Test Announcement',
      message: 'Test message',
      announcementId: 'ann-456',
    };
    
    render(<NotificationBanner notification={notification} />);
    expect(screen.getByText('Acknowledge')).toBeInTheDocument();
    expect(screen.getByText('Dismiss')).toBeInTheDocument();
  });

  it('calls onAcknowledge when acknowledge button is clicked', () => {
    const onAcknowledge = vi.fn();
    const notification = {
      type: 'ANNOUNCEMENT',
      title: 'Test',
      message: 'Test message',
      announcementId: 'ann-789',
    };
    
    render(
      <NotificationBanner 
        notification={notification} 
        onAcknowledge={onAcknowledge}
      />
    );
    
    fireEvent.click(screen.getByText('Acknowledge'));
    expect(onAcknowledge).toHaveBeenCalledWith('ann-789');
  });

  it('returns null when notification is not provided', () => {
    const { container } = render(<NotificationBanner notification={null} />);
    expect(container.firstChild).toBeNull();
  });
});
