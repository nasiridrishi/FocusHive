module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
    'plugin:jsx-a11y/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh', 'jsx-a11y'],
  rules: {
    // Console logging
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    
    // Strict TypeScript rules
    '@typescript-eslint/no-explicit-any': ['error', { 
      ignoreRestArgs: true,
      fixToUnknown: true 
    }],
    '@typescript-eslint/explicit-function-return-type': ['warn', {
      allowExpressions: true,
      allowTypedFunctionExpressions: true,
      allowHigherOrderFunctions: true,
      allowDirectConstAssertionInArrowFunctions: true,
    }],
    '@typescript-eslint/no-non-null-assertion': 'error',
    '@typescript-eslint/strict-boolean-expressions': 'off', // Too strict for now
    '@typescript-eslint/no-floating-promises': 'off', // Requires parserOptions.project
    
    // Best practices
    'no-debugger': 'error',
    'no-alert': 'warn',
    'prefer-const': 'error',
    'no-var': 'error',
    'eqeqeq': ['error', 'always', { null: 'ignore' }],
    'curly': ['error', 'multi-line'],
    
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
  },
  
  // Override rules for different file types
  overrides: [
    // Test files (unit/integration tests)
    {
      files: ['**/*.test.{ts,tsx,js,jsx}', '**/*.spec.{ts,tsx,js,jsx}'],
      env: {
        jest: true,
        node: true,
        es6: true
      },
      globals: {
        // Vitest globals
        describe: 'readonly',
        it: 'readonly',
        test: 'readonly',
        expect: 'readonly',
        vi: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        beforeAll: 'readonly',
        afterAll: 'readonly'
      },
      rules: {
        // Allow console.log in tests for debugging
        'no-console': 'off',
        
        // More flexible naming in tests
        '@typescript-eslint/naming-convention': 'off',
        
        // Allow any type in test mocks and fixtures
        '@typescript-eslint/no-explicit-any': 'warn',
        
        // Allow unused variables for test setup
        '@typescript-eslint/no-unused-vars': [
          'error',
          {
            argsIgnorePattern: '^_|^unused',
            varsIgnorePattern: '^_|^unused|^mock',
            ignoreRestSiblings: true,
            destructuredArrayIgnorePattern: '^_'
          }
        ],
        
        // Allow empty functions in mocks
        '@typescript-eslint/no-empty-function': 'off',
        
        // Allow non-null assertions in tests
        '@typescript-eslint/no-non-null-assertion': 'warn',
        
        // Allow importing from devDependencies in tests
        'import/no-extraneous-dependencies': 'off',
      }
    },
    
    // Test utilities and setup files
    {
      files: [
        'src/test-utils/**/*',
        'src/test-setup.ts',
        'src/**/*mock*',
        '**/__mocks__/**/*',
        '**/setupTests.*',
        '**/vitest.config.*',
        '**/jest.config.*'
      ],
      rules: {
        // Allow console for test utilities
        'no-console': 'off',
        
        // Flexible naming for test utilities
        '@typescript-eslint/naming-convention': 'off',
        
        // Allow any type for test utilities
        '@typescript-eslint/no-explicit-any': 'warn',
        
        // Allow unused parameters in test utilities
        '@typescript-eslint/no-unused-vars': [
          'error',
          {
            argsIgnorePattern: '^_|^unused',
            varsIgnorePattern: '^_|^unused',
            ignoreRestSiblings: true
          }
        ],
        
        // Allow importing from devDependencies
        'import/no-extraneous-dependencies': 'off',
      }
    },
    
    // E2E test files (Playwright)
    {
      files: [
        'e2e/**/*',
        '**/*.e2e.{ts,js}',
        '**/playwright.config.*'
      ],
      env: {
        node: true,
        es2020: true
      },
      rules: {
        // Allow console.log for E2E debugging
        'no-console': 'off',
        
        // More flexible naming for E2E tests and page objects
        '@typescript-eslint/naming-convention': [
          'error',
          // Allow PascalCase for page objects and helpers
          {
            selector: 'class',
            format: ['PascalCase']
          },
          // Allow camelCase and PascalCase for variables/functions
          {
            selector: ['variable', 'function'],
            format: ['camelCase', 'PascalCase', 'UPPER_CASE'],
            leadingUnderscore: 'allow'
          },
          // Allow any format for properties (test data, selectors, etc.)
          {
            selector: 'property',
            format: null
          }
        ],
        
        // Allow any type for E2E test data and page interactions
        '@typescript-eslint/no-explicit-any': 'warn',
        
        // Allow unused parameters in E2E helpers
        '@typescript-eslint/no-unused-vars': [
          'error',
          {
            argsIgnorePattern: '^_|^unused',
            varsIgnorePattern: '^_|^unused',
            ignoreRestSiblings: true
          }
        ],
        
        // Allow empty functions in page object stubs
        '@typescript-eslint/no-empty-function': 'off',
        
        // Allow non-null assertions in E2E tests
        '@typescript-eslint/no-non-null-assertion': 'warn',
        
        // Allow importing from devDependencies
        'import/no-extraneous-dependencies': 'off',
      }
    },
    
    // Configuration files
    {
      files: [
        '*.config.{ts,js,cjs,mjs}',
        '.eslintrc.*',
        'vite.config.*',
        'tailwind.config.*'
      ],
      env: {
        node: true
      },
      rules: {
        // Allow console in config files
        'no-console': 'off',
        
        // Allow any type in configurations
        '@typescript-eslint/no-explicit-any': 'warn',
        
        // Allow importing from devDependencies in configs
        'import/no-extraneous-dependencies': 'off',
        
        // Allow unused vars in configs (sometimes needed for type inference)
        '@typescript-eslint/no-unused-vars': [
          'error',
          {
            argsIgnorePattern: '^_|^unused',
            varsIgnorePattern: '^_|^unused',
            ignoreRestSiblings: true
          }
        ]
      }
    }
  ]
};