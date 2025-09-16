/**
 * E2E Test Setup - NO MOCKS ALLOWED
 *
 * This setup file is for E2E tests that connect to REAL services.
 *
 * REQUIREMENTS:
 * - NO MSW server
 * - NO axios mocks
 * - NO service mocks
 * - REAL Identity Service at http://localhost:8081
 * - REAL Backend at http://localhost:8080
 * - REAL WebSocket at ws://localhost:8080/ws
 */

import '@testing-library/jest-dom';
import { afterAll, beforeAll, beforeEach, afterEach, describe, expect, it, vi } from 'vitest';

// Set up environment variables for E2E tests if not already set
process.env.VITE_API_BASE_URL = process.env.VITE_API_BASE_URL || 'http://localhost:8080';
process.env.VITE_IDENTITY_API_URL = process.env.VITE_IDENTITY_API_URL || 'http://localhost:8081';
process.env.VITE_WEBSOCKET_URL = process.env.VITE_WEBSOCKET_URL || 'ws://localhost:8080';
process.env.VITE_WEBSOCKET_RECONNECT_ATTEMPTS = process.env.VITE_WEBSOCKET_RECONNECT_ATTEMPTS || '5';
process.env.VITE_WEBSOCKET_RECONNECT_DELAY = process.env.VITE_WEBSOCKET_RECONNECT_DELAY || '5000';
process.env.VITE_WEBSOCKET_HEARTBEAT_INTERVAL = process.env.VITE_WEBSOCKET_HEARTBEAT_INTERVAL || '30000';

// Make test globals available
(globalThis as Record<string, unknown>).beforeEach = beforeEach;
(globalThis as Record<string, unknown>).afterEach = afterEach;
(globalThis as Record<string, unknown>).beforeAll = beforeAll;
(globalThis as Record<string, unknown>).afterAll = afterAll;
(globalThis as Record<string, unknown>).describe = describe;
(globalThis as Record<string, unknown>).it = it;
(globalThis as Record<string, unknown>).expect = expect;
(globalThis as Record<string, unknown>).vi = vi;

// Setup localStorage and sessionStorage mocks for jsdom
const localStorageMock: Storage = (() => {
  let store: Record<string, string> = {};

  return {
    getItem(key: string): string | null {
      return store[key] || null;
    },
    setItem(key: string, value: string): void {
      store[key] = value.toString();
    },
    removeItem(key: string): void {
      delete store[key];
    },
    clear(): void {
      store = {};
    },
    get length(): number {
      return Object.keys(store).length;
    },
    key(index: number): string | null {
      const keys = Object.keys(store);
      return keys[index] || null;
    }
  };
})();

const sessionStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem(key: string): string | null {
      return store[key] || null;
    },
    setItem(key: string, value: string): void {
      store[key] = value.toString();
    },
    removeItem(key: string): void {
      delete store[key];
    },
    clear(): void {
      store = {};
    },
    get length(): number {
      return Object.keys(store).length;
    },
    key(index: number): string | null {
      const keys = Object.keys(store);
      return keys[index] || null;
    }
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
});

Object.defineProperty(window, 'sessionStorage', {
  value: sessionStorageMock
});

// Global test utilities (minimal, only what's absolutely needed)
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
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock canvas context for potential chart tests
HTMLCanvasElement.prototype.getContext = vi.fn().mockReturnValue({
  fillRect: vi.fn(),
  clearRect: vi.fn(),
  getImageData: vi.fn(() => ({data: new Array(4)})),
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
  measureText: vi.fn(() => ({width: 0})),
  transform: vi.fn(),
  rect: vi.fn(),
  clip: vi.fn(),
});

// Log environment for debugging
console.log('E2E Test Setup Loaded - NO MOCKS');
console.log('Expected services:');
console.log('- Identity Service: http://localhost:8081');
console.log('- Backend Service: http://localhost:8080');
console.log('- WebSocket: ws://localhost:8080/ws');