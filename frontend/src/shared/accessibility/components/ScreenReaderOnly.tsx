/**
 * Screen Reader Only Component
 * 
 * Visually hides content while keeping it available to screen readers.
 * Useful for providing additional context or instructions for assistive technology.
 */

import React from 'react';
import { styled } from '@mui/material/styles';
import { Box } from '@mui/material';

/**
 * Styled component that hides content visually but keeps it accessible to screen readers
 */
const VisuallyHidden = styled(Box)(({ theme }) => ({
  position: 'absolute',
  width: '1px',
  height: '1px',
  padding: 0,
  margin: '-1px',
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap',
  border: 0,
  
  // Ensure text is readable if somehow becomes visible
  color: theme.palette.text.primary,
  backgroundColor: 'transparent',
  
  // Focus styles for elements that can receive focus
  '&:focus': {
    position: 'static',
    width: 'auto',
    height: 'auto',
    padding: theme.spacing(0.5),
    margin: 0,
    overflow: 'visible',
    clip: 'auto',
    whiteSpace: 'normal',
    backgroundColor: theme.palette.background.paper,
    border: `2px solid ${theme.palette.primary.main}`,
    borderRadius: theme.shape.borderRadius,
    zIndex: theme.zIndex.tooltip,
  },
}));

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
  [key: string]: any;
}

/**
 * Screen Reader Only Component
 * 
 * Use this component to provide additional context or instructions
 * that are only needed for screen reader users.
 */
export const ScreenReaderOnly: React.FC<ScreenReaderOnlyProps> = ({
  children,
  as: Component = 'span',
  focusable = false,
  ...props
}) => {
  return (
    <VisuallyHidden
      component={Component}
      tabIndex={focusable ? 0 : undefined}
      {...props}
    >
      {children}
    </VisuallyHidden>
  );
};

/**
 * Screen Reader Instructions Component
 * 
 * Provides specific instructions for screen reader users
 */
export const ScreenReaderInstructions: React.FC<{
  children: React.ReactNode;
  id?: string;
}> = ({ children, id }) => (
  <ScreenReaderOnly as="div" id={id} role="note">
    {children}
  </ScreenReaderOnly>
);

/**
 * Screen Reader Label Component
 * 
 * Provides additional labeling context for form elements
 */
export const ScreenReaderLabel: React.FC<{
  children: React.ReactNode;
  htmlFor?: string;
  id?: string;
}> = ({ children, htmlFor, id }) => (
  <ScreenReaderOnly as="label" htmlFor={htmlFor} id={id}>
    {children}
  </ScreenReaderOnly>
);

/**
 * Screen Reader Description Component
 * 
 * Provides additional description for complex elements
 */
export const ScreenReaderDescription: React.FC<{
  children: React.ReactNode;
  id?: string;
}> = ({ children, id }) => (
  <ScreenReaderOnly as="div" id={id}>
    {children}
  </ScreenReaderOnly>
);

/**
 * Live Region Component
 * 
 * Creates a visually hidden live region for announcements
 */
export const ScreenReaderLiveRegion: React.FC<{
  children: React.ReactNode;
  level?: 'polite' | 'assertive';
  atomic?: boolean;
  relevant?: 'additions' | 'removals' | 'text' | 'all';
  id?: string;
}> = ({ 
  children, 
  level = 'polite', 
  atomic = true, 
  relevant = 'all',
  id 
}) => (
  <ScreenReaderOnly
    as="div"
    id={id}
    aria-live={level}
    aria-atomic={atomic}
    aria-relevant={relevant}
  >
    {children}
  </ScreenReaderOnly>
);

/**
 * Status Region Component
 * 
 * Creates a status region for non-urgent updates
 */
export const ScreenReaderStatus: React.FC<{
  children: React.ReactNode;
  id?: string;
}> = ({ children, id }) => (
  <ScreenReaderOnly as="div" id={id} role="status" aria-live="polite">
    {children}
  </ScreenReaderOnly>
);

/**
 * Alert Region Component
 * 
 * Creates an alert region for urgent announcements
 */
export const ScreenReaderAlert: React.FC<{
  children: React.ReactNode;
  id?: string;
}> = ({ children, id }) => (
  <ScreenReaderOnly as="div" id={id} role="alert" aria-live="assertive">
    {children}
  </ScreenReaderOnly>
);

/**
 * Hook for managing screen reader only content
 */
export function useScreenReaderOnly(initialContent: string = '') {
  const [content, setContent] = React.useState(initialContent);
  const [isVisible, setIsVisible] = React.useState(false);

  const updateContent = React.useCallback((newContent: string) => {
    setContent(newContent);
  }, []);

  const showTemporarily = React.useCallback((duration: number = 3000) => {
    setIsVisible(true);
    setTimeout(() => setIsVisible(false), duration);
  }, []);

  const clear = React.useCallback(() => {
    setContent('');
  }, []);

  return {
    content,
    updateContent,
    clear,
    isVisible,
    showTemporarily,
    Component: React.useCallback(
      ({ children, ...props }: { children?: React.ReactNode; [key: string]: any }) => (
        <ScreenReaderOnly {...props}>
          {children || content}
        </ScreenReaderOnly>
      ),
      [content]
    )
  };
}

/**
 * Utility function to create screen reader only content
 */
export const createScreenReaderContent = (content: string) => (
  <ScreenReaderOnly>{content}</ScreenReaderOnly>
);

export default ScreenReaderOnly;