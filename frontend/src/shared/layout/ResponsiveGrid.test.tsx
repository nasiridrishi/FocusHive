/**
 * Test for ResponsiveGrid components to verify infinite recursion fix
 */

import React from 'react';
import { render } from '@testing-library/react';
import { describe, test, expect, vi } from 'vitest';
import { ThemeProvider } from '@mui/material/styles';
import { theme } from '../theme';
import { ResponsiveGrid, GridItem } from './ResponsiveGrid';

// Mock the hooks to avoid dependency issues in tests
vi.mock('../hooks', () => ({
  useContainerQuery: () => ({
    containerRef: { current: null },
    responsiveValue: (_values: any, defaultValue: any) => defaultValue,
  }),
  useResponsive: () => ({
    currentBreakpoint: 'desktop' as const,
  }),
}));

describe('ResponsiveGrid', () => {
  const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
    <ThemeProvider theme={theme}>
      {children}
    </ThemeProvider>
  );

  test('ResponsiveGrid renders without infinite recursion', () => {
    expect(() => {
      render(
        <TestWrapper>
          <ResponsiveGrid columns={2} gap={1}>
            <div>Item 1</div>
            <div>Item 2</div>
          </ResponsiveGrid>
        </TestWrapper>
      );
    }).not.toThrow();
  });

  test('GridItem renders without infinite recursion', () => {
    expect(() => {
      render(
        <TestWrapper>
          <ResponsiveGrid columns={2}>
            <GridItem span={1}>
              <div>Grid Item Content</div>
            </GridItem>
            <GridItem span={1}>
              <div>Another Grid Item</div>
            </GridItem>
          </ResponsiveGrid>
        </TestWrapper>
      );
    }).not.toThrow();
  });

  test('GridItem with responsive span values', () => {
    expect(() => {
      render(
        <TestWrapper>
          <ResponsiveGrid>
            <GridItem span={{ mobile: 1, tablet: 2, desktop: 3 }}>
              <div>Responsive Grid Item</div>
            </GridItem>
          </ResponsiveGrid>
        </TestWrapper>
      );
    }).not.toThrow();
  });

  test('GridItem with advanced properties', () => {
    expect(() => {
      render(
        <TestWrapper>
          <ResponsiveGrid>
            <GridItem 
              span={2} 
              spanRow={1} 
              order={1} 
              align="center" 
              justify="center"
            >
              <div>Advanced Grid Item</div>
            </GridItem>
          </ResponsiveGrid>
        </TestWrapper>
      );
    }).not.toThrow();
  });
});