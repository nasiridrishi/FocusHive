module.exports = {
  preset: 'ts-jest',
  testMatch: ['<rootDir>/tests/**/*.test.ts'],
  testEnvironment: 'node',
  collectCoverageFrom: ['packages/*/src/**/*.{js,ts}', '!**/node_modules/**', '!**/dist/**'],
};
