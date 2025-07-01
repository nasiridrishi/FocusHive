module.exports = {
  testMatch: ['<rootDir>/tests/**/*.test.js'],
  testEnvironment: 'node',
  collectCoverageFrom: [
    'packages/*/src/**/*.{js,ts}',
    '!**/node_modules/**',
    '!**/dist/**'
  ]
};