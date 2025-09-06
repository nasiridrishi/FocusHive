import React from 'react';

export interface ScreenReaderOnlyProps {
  /**
   * Content to be hidden from visual display but available to screen readers
   */
  children: React.ReactNode;
  
  /**
   * HTML element type to render
   * @default 'span'
   */
  as?: 'span' | 'div' | 'p' | 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'label' | 'legend';
  
  /**
   * Whether the element can receive focus (useful for skip links)
   * @default false
   */
  focusable?: boolean;
  
  /**
   * Additional props to pass to the underlying element
   */
  [key: string]: unknown;
}

// Note: useScreenReaderOnly hook cannot be moved here due to circular dependency with ScreenReaderOnly component
// It must remain in the component file

// Simple utility for creating screen reader content without component dependency
export const createScreenReaderSpan = (content: string) => (
  <span 
    style={{
      position: 'absolute',
      width: '1px',
      height: '1px',
      padding: 0,
      margin: '-1px',
      overflow: 'hidden',
      clip: 'rect(0, 0, 0, 0)',
      whiteSpace: 'nowrap',
      border: 0,
    }}
    aria-live="polite"
  >
    {content}
  </span>
);