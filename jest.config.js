module.exports = {
  preset: 'ts-jest',
  testMatch: ['<rootDir>/tests/**/*.test.ts'],
  testEnvironment: 'node',
  collectCoverageFrom: [
    'packages/*/src/**/*.{js,ts,jsx,tsx}',
    '!**/node_modules/**',
    '!**/dist/**',
    '!**/__tests__/**',
    '!**/test/**',
    '!**/*.d.ts',
    '!**/index.ts',
  ],
  coverageDirectory: '<rootDir>/coverage',
  coverageReporters: ['text', 'lcov', 'html', 'json-summary'],
  coverageThreshold: {
    global: {
      branches: 60,
      functions: 60,
      lines: 60,
      statements: 60,
    },
  },
  projects: [
    '<rootDir>/packages/backend/jest.config.js',
    '<rootDir>/packages/frontend/jest.config.js',
    '<rootDir>/packages/shared/jest.config.js',
  ],
};
