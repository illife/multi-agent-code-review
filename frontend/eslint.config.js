import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-case-declarations': 'warn',
      'no-useless-escape': 'warn',
      'react-hooks/immutability': 'warn',
      'react-hooks/purity': 'warn',
      'no-restricted-globals': [
        'error',
        {
          name: 'fetch',
          message: 'Use the shared axios instance from src/services/api.ts for HTTP requests.',
        },
      ],
    },
  },
])
