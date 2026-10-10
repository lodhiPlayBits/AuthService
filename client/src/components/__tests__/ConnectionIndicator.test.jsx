import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import ConnectionIndicator from '../ConnectionIndicator';

describe('ConnectionIndicator', () => {
  it('renders connected state', () => {
    const { container } = render(<ConnectionIndicator connectionState="connected" />);
    expect(container.firstChild).toBeInTheDocument();
  });

  it('renders connecting state', () => {
    const { container } = render(<ConnectionIndicator connectionState="connecting" />);
    expect(container.firstChild).toBeInTheDocument();
  });

  it('renders disconnected state', () => {
    const { container } = render(<ConnectionIndicator connectionState="disconnected" />);
    expect(container.firstChild).toBeInTheDocument();
  });

  it('renders error state', () => {
    const { container } = render(<ConnectionIndicator connectionState="error" />);
    expect(container.firstChild).toBeInTheDocument();
  });
});
