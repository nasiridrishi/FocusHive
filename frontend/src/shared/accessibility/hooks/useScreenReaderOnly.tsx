import React, { useState, useCallback } from 'react';
import { ScreenReaderOnly } from '../components/ScreenReaderOnly';

/**
 * Hook for managing screen reader only content
 */
export function useScreenReaderOnly(initialContent: string = '') {
  const [content, setContent] = useState(initialContent);
  const [isVisible, setIsVisible] = useState(false);

  const updateContent = useCallback((newContent: string) => {
    setContent(newContent);
  }, []);

  const showTemporarily = useCallback((duration: number = 3000) => {
    setIsVisible(true);
    setTimeout(() => setIsVisible(false), duration);
  }, []);

  const clear = useCallback(() => {
    setContent('');
  }, []);

  return {
    content,
    updateContent,
    clear,
    isVisible,
    showTemporarily,
    Component: useCallback(
      ({ children, ...props }: { children?: React.ReactNode; [key: string]: unknown }) => (
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