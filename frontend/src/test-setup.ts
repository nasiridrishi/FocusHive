import '@testing-library/jest-dom';
import React from 'react';
import { vi, beforeEach, afterEach, describe, it, expect } from 'vitest';

// Mock axios first before any other imports
vi.mock('axios', () => {
  const mockAxios: Record<string, unknown> = {
    create: vi.fn(() => mockAxios),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
    interceptors: {
      request: {
        use: vi.fn(),
        eject: vi.fn(),
      },
      response: {
        use: vi.fn(),
        eject: vi.fn(),
      },
    },
  };
  return {
    default: mockAxios,
    ...mockAxios,
  } as Record<string, unknown>;
});

// Make test globals available
(globalThis as Record<string, unknown>).beforeEach = beforeEach;
(globalThis as Record<string, unknown>).afterEach = afterEach; 
(globalThis as Record<string, unknown>).describe = describe;
(globalThis as Record<string, unknown>).it = it;
(globalThis as Record<string, unknown>).expect = expect;
(globalThis as Record<string, unknown>).vi = vi;

// Mock MUI X Charts
vi.mock('@mui/x-charts', () => ({
  LineChart: vi.fn(({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>) => {
    return React.createElement('div', {
      'data-testid': 'line-chart',
      'data-props': JSON.stringify(props)
    }, children);
  }),
  BarChart: vi.fn(({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>) => {
    return React.createElement('div', {
      'data-testid': 'bar-chart', 
      'data-props': JSON.stringify(props)
    }, children);
  }),
  ResponsiveChartContainer: vi.fn(({ children, ...props }: React.PropsWithChildren<Record<string, unknown>>) => {
    return React.createElement('div', {
      'data-testid': 'chart-container',
      'data-props': JSON.stringify(props)
    }, children);
  }),
  ChartsTooltip: vi.fn(() => React.createElement('div', { 'data-testid': 'chart-tooltip' })),
  ChartsAxisHighlight: vi.fn(() => React.createElement('div', { 'data-testid': 'chart-axis-highlight' })),
  ChartsXAxis: vi.fn(() => React.createElement('div', { 'data-testid': 'chart-x-axis' })),
  ChartsYAxis: vi.fn(() => React.createElement('div', { 'data-testid': 'chart-y-axis' }))
}));

// Mock MUI X Date Pickers
vi.mock('@mui/x-date-pickers', () => ({
  LocalizationProvider: vi.fn(({ children }: React.PropsWithChildren) => React.createElement('div', {}, children)),
  DatePicker: vi.fn(({ label, onChange, value, slotProps }: { label?: string; onChange?: (date: Date | null) => void; value?: Date | null; slotProps?: Record<string, unknown> }) => {
    return React.createElement('input', {
      'aria-label': label,
      type: 'date',
      value: value ? value.toISOString().split('T')[0] : '',
      onChange: (e: React.ChangeEvent<HTMLInputElement>) => onChange && onChange(new Date(e.target.value)),
      ...(slotProps?.textField || {})
    });
  })
}));

// Mock MUI x-date-pickers specific modules to prevent import errors
vi.mock('@mui/x-date-pickers/DatePicker', () => ({
  DatePicker: vi.fn(({ label, onChange, value, slotProps }: { label?: string; onChange?: (date: Date | null) => void; value?: Date | null; slotProps?: Record<string, unknown> }) => {
    return React.createElement('input', {
      'aria-label': label,
      type: 'date',
      value: value ? value.toISOString().split('T')[0] : '',
      onChange: (e: React.ChangeEvent<HTMLInputElement>) => onChange && onChange(new Date(e.target.value)),
      ...(slotProps?.textField || {})
    });
  })
}));

// Mock date-fns adapter
vi.mock('@mui/x-date-pickers/AdapterDateFns', () => ({
  AdapterDateFns: class MockAdapterDateFns {}
}));

// Mock framer-motion to prevent DOM prop warnings
const createMotionComponent = (tag: string) => 
  React.forwardRef<HTMLElement, React.PropsWithChildren<Record<string, unknown>>>(({ children, ...props }, ref) => {
    // Filter out framer-motion specific props
    const { 
      animate, initial, exit, variants, transition, 
      whileHover, whileTap, whileFocus, whileInView,
      drag, dragConstraints, dragElastic, dragMomentum, 
      dragTransition, dragControls,
      layout, layoutId, layoutDependency, layoutScroll, layoutRoot,
      onAnimationStart, onAnimationComplete, onUpdate, 
      onDragStart, onDragEnd, onDrag,
      onDirectionLock, onViewportEnter, onViewportLeave, 
      onHoverStart, onHoverEnd,
      onTap, onTapStart, onTapCancel, onFocus, onBlur, 
      onDragTransitionEnd,
      style, transformTemplate, transformValues, ...filteredProps 
    } = props;
    
    // Silence unused variable warnings by acknowledging them
    void animate; void initial; void exit; void variants; void transition;
    void whileHover; void whileTap; void whileFocus; void whileInView;
    void drag; void dragConstraints; void dragElastic; void dragMomentum;
    void dragTransition; void dragControls;
    void layout; void layoutId; void layoutDependency; void layoutScroll; void layoutRoot;
    void onAnimationStart; void onAnimationComplete; void onUpdate;
    void onDragStart; void onDragEnd; void onDrag;
    void onDirectionLock; void onViewportEnter; void onViewportLeave;
    void onHoverStart; void onHoverEnd;
    void onTap; void onTapStart; void onTapCancel; void onFocus; void onBlur;
    void onDragTransitionEnd;
    void style; void transformTemplate; void transformValues;
    
    return React.createElement(tag, { ...filteredProps, ref }, children as React.ReactNode);
  });

vi.mock('framer-motion', () => ({
  motion: {
    div: createMotionComponent('div'),
    span: createMotionComponent('span'),
    img: createMotionComponent('img'),
    button: createMotionComponent('button'),
    section: createMotionComponent('section'),
    article: createMotionComponent('article'),
    nav: createMotionComponent('nav'),
    aside: createMotionComponent('aside'),
    header: createMotionComponent('header'),
    footer: createMotionComponent('footer'),
    main: createMotionComponent('main'),
    p: createMotionComponent('p'),
    h1: createMotionComponent('h1'),
    h2: createMotionComponent('h2'),
    h3: createMotionComponent('h3'),
    h4: createMotionComponent('h4'),
    h5: createMotionComponent('h5'),
    h6: createMotionComponent('h6'),
  },
  AnimatePresence: ({ children }: React.PropsWithChildren) => children,
  useAnimation: () => ({}),
  useMotionValue: (value: unknown) => ({ get: () => value, set: () => {} }),
  useTransform: (value: unknown, input: unknown, output: unknown) => {
    void input; void output; // Acknowledge unused parameters
    return value;
  },
  useSpring: (value: unknown) => value,
  useInView: () => true,
  useDragControls: () => ({}),
}));

// Mock PWA virtual modules
vi.mock('virtual:pwa-register', () => ({
  registerSW: vi.fn(() => {
    return vi.fn(); // Mock update function
  }),
}));

// Mock MUI useMediaQuery hook and other imports
vi.mock('@mui/material', async () => {
  const actual = await vi.importActual('@mui/material');
  return {
    ...actual,
    useMediaQuery: vi.fn(() => false), // Default to false, can be overridden in tests
  };
});

// Mock MUI useMediaQuery as a standalone import to prevent directory import errors
vi.mock('@mui/material/useMediaQuery', () => ({
  default: vi.fn(() => false),
  __esModule: true,
}));

// Mock MUI styles directory imports to prevent ESM errors
vi.mock('@mui/material/styles', async () => {
  const actual = await vi.importActual('@mui/material/styles');
  return {
    ...actual,
    responsiveFontSizes: vi.fn((theme: unknown) => theme), // Return theme as-is to avoid line-height issues
  };
});

// Mock MUI styles directory for x-date-pickers
vi.mock('@mui/material/node/styles', async () => {
  const actual = await vi.importActual('@mui/material/styles');
  return actual;
});

// Global test utilities
globalThis.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}));

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Axios mock is already defined at the top of the file

// Mock canvas context for charts
HTMLCanvasElement.prototype.getContext = vi.fn().mockReturnValue({
  fillRect: vi.fn(),
  clearRect: vi.fn(),
  getImageData: vi.fn(() => ({ data: new Array(4) })),
  putImageData: vi.fn(),
  createImageData: vi.fn(() => []),
  setTransform: vi.fn(),
  drawImage: vi.fn(),
  save: vi.fn(),
  fillText: vi.fn(),
  restore: vi.fn(),
  beginPath: vi.fn(),
  moveTo: vi.fn(),
  lineTo: vi.fn(),
  closePath: vi.fn(),
  stroke: vi.fn(),
  translate: vi.fn(),
  scale: vi.fn(),
  rotate: vi.fn(),
  arc: vi.fn(),
  fill: vi.fn(),
  measureText: vi.fn(() => ({ width: 0 })),
  transform: vi.fn(),
  rect: vi.fn(),
  clip: vi.fn(),
});