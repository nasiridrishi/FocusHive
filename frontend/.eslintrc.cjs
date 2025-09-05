module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh'],
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
    
    // Naming Convention Rules for FocusHive
    '@typescript-eslint/naming-convention': [
      'error',
      // Components should be PascalCase
      {
        selector: ['function'],
        filter: {
          regex: '^[A-Z]',
          match: true,
        },
        format: ['PascalCase'],
      },
      // Regular functions should be camelCase (but allow PascalCase for React components)
      {
        selector: ['function'],
        format: ['camelCase', 'PascalCase'],
      },
      // Variables should be camelCase
      {
        selector: ['variable'],
        format: ['camelCase'],
        leadingUnderscore: 'allow',
      },
      // Constants should be UPPER_CASE or camelCase (for non-primitive constants)
      {
        selector: ['variable'],
        modifiers: ['const', 'global'],
        format: ['UPPER_CASE', 'camelCase', 'PascalCase'],
      },
      // Type-like constructs should be PascalCase
      {
        selector: ['typeLike'],
        format: ['PascalCase'],
      },
      // Interfaces should be PascalCase
      {
        selector: ['interface'],
        format: ['PascalCase'],
      },
      // Enums should be PascalCase
      {
        selector: ['enum'],
        format: ['PascalCase'],
      },
      // Enum members should be UPPER_CASE
      {
        selector: ['enumMember'],
        format: ['UPPER_CASE'],
      },
      // Parameters should be camelCase
      {
        selector: ['parameter'],
        format: ['camelCase'],
        leadingUnderscore: 'allow',
      },
      // Methods should be camelCase
      {
        selector: ['method'],
        format: ['camelCase'],
      },
      // Properties should be camelCase but allow PascalCase for React component props
      // CSS-in-JS selectors (string literals) are exempt from naming conventions
      {
        selector: ['property'],
        format: ['camelCase', 'PascalCase', 'UPPER_CASE'],
        filter: {
          regex: '^(&\\s|@|:)',
          match: false,
        },
      },
      // Allow CSS-in-JS selectors and pseudo-selectors to use any format when quoted
      {
        selector: ['property'],
        format: null,
        filter: {
          regex: '^(&\\s|@|:|\\.|#)',
          match: true,
        },
      },
    ],

    // Disable conflicting camelcase rule in favor of naming-convention
    'camelcase': 'off',
  },
};