import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AuroraBackground from '../AuroraBackground';

describe('AuroraBackground', () => {
  it('renders children correctly', () => {
    render(
      <AuroraBackground>
        <div>Test Content</div>
      </AuroraBackground>
    );
    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('renders aurora gradient elements', () => {
    const { container } = render(
      <AuroraBackground>
        <div>Test</div>
      </AuroraBackground>
    );
    
    const gradients = container.querySelectorAll('.aurora-gradient');
    expect(gradients).toHaveLength(3);
  });

  it('renders aurora wrapper', () => {
    const { container } = render(
      <AuroraBackground>
        <div>Test</div>
      </AuroraBackground>
    );
    
    expect(container.querySelector('.aurora-wrapper')).toBeInTheDocument();
  });
});
