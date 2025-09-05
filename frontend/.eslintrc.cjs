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
      // Allow CSS selectors, HTML attributes, API properties, and special patterns
      {
        selector: 'objectLiteralProperty',
        format: null,
        filter: {
          // Match patterns that should be exempt from naming convention
          // Updated to include: MUI selectors, percentages, CSS variables, HTML elements, snake_case, numeric keys, MIME types, attribute selectors, version strings, CSS selectors, universal selectors, decimal numbers, paths, kebab-case, virtual modules
          regex: '^(--[a-z-]+|&\\s*\\.|&\\s+[a-z]+|&\\s*>|&\\[|aria-|data-|Content-Type|fieldset|[0-9]+%|[0-9]+%,\\s*[0-9]+%|[0-9]+%,\\s*[0-9]+%,\\s*[0-9]+%|__[a-zA-Z]+|webkit-|moz-|ms-|o-|[a-z]+_[a-z_]+|^[0-9]+$|[a-z]+/[a-z]+|^_[a-zA-Z]|^[0-9]+\\.[0-9]+\\.[0-9]+$|^[0-9]+\\.[0-9]+$|\\*|:focus|:not\\(|^/[a-zA-Z]+|^[a-z]+-[a-z-]+$|virtual:)',
          match: true
        }
      },
      // Allow CSS pseudo-selectors and at-rules
      {
        selector: 'objectLiteralProperty',
        filter: {
          regex: '^(&:|@|:hover|:focus|:active|:disabled|\\.|#)',
          match: true
        },
        format: null
      },
      // Allow method names like AnimatePresence (React component names)
      {
        selector: 'objectLiteralMethod',
        format: ['camelCase', 'PascalCase'],
      },
      // Standard camelCase for other object properties, allow HTML attributes  
      {
        selector: 'objectLiteralProperty',
        format: ['camelCase', 'PascalCase', 'UPPER_CASE'],
        filter: {
          // Allow HTML attributes like aria-label, data-*, etc.
          regex: '^(aria-|data-|webkit-|moz-|ms-|o-|align-|background-|border-|box-|color-|cursor-|display-|flex-|font-|grid-|height-|justify-|line-|margin-|max-|min-|outline-|overflow-|padding-|position-|text-|transform-|transition-|white-|width-|z-index)',
          match: false
        }
      },
      // Type properties with special patterns (HTML attributes, API properties, etc.)
      {
        selector: 'typeProperty',
        format: null,
        filter: {
          // Allow HTML/ARIA attributes, API properties, CSS variables, event names with colons, etc.
          regex: '^(aria-|data-|Content-Type|--[a-z-]+|_|[a-z]+:[a-z-]+)',
          match: true
        }
      },
      // Type properties (in interfaces/types) can use snake_case for API compatibility
      {
        selector: 'typeProperty',
        format: ['camelCase', 'snake_case', 'PascalCase', 'UPPER_CASE'],
        leadingUnderscore: 'allow'
      },
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
      // Variables should be camelCase, PascalCase, or UPPER_CASE for constants
      {
        selector: ['variable'],
        format: ['camelCase', 'PascalCase', 'UPPER_CASE'],
        leadingUnderscore: 'allow',
        filter: {
          // Allow test variables starting with underscore
          regex: '^_[a-zA-Z]',
          match: false
        }
      },
      // Allow test variables with underscore prefix
      {
        selector: ['variable'],
        filter: {
          regex: '^_[a-zA-Z]',
          match: true
        },
        format: null
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
      // Parameters should be camelCase, but allow PascalCase for React components
      {
        selector: ['parameter'],
        format: ['camelCase', 'PascalCase'],
        leadingUnderscore: 'allow',
      },
      // Methods should be camelCase
      {
        selector: ['method'],
        format: ['camelCase'],
      }
    ],

    // Disable conflicting camelcase rule in favor of naming-convention
    'camelcase': 'off',
    
    // Allow unused variables with underscore prefix
    '@typescript-eslint/no-unused-vars': [
      'error',
      { 
        argsIgnorePattern: '^_',
        varsIgnorePattern: '^_',
        ignoreRestSiblings: true 
      }
    ],
    
    // Allow any type in specific cases but prefer unknown
    '@typescript-eslint/no-explicit-any': [
      'error',
      { 
        ignoreRestArgs: true,
        fixToUnknown: true 
      }
    ],
  },
};