import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: ['./src/test-utils/setupTests.ts'],
    css: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/**',
        'src/test-utils/**',
        'src/tests/**',
        '**/*.d.ts',
        'src/main.tsx',
        'src/vite-env.d.ts',
        '**/__tests__/**',
        '**/*.test.{ts,tsx}',
        '**/*.spec.{ts,tsx}',
        'coverage/**',
        'dist/**',
        '.eslintrc.cjs',
        'postcss.config.js',
        'tailwind.config.js',
        'vite.config.ts',
        'vitest.config.ts',
      ],
      include: ['src/**/*.{ts,tsx}'],
      thresholds: {
        global: {
          branches: 80,
          functions: 80,
          lines: 80,
          statements: 80,
        },
      },
    },
    include: [
      'src/tests/unit/**/*.test.{ts,tsx}',
      'src/tests/integration/**/*.test.{ts,tsx}',
      'src/tests/performance/**/*.test.{ts,tsx}',
      'src/tests/accessibility/**/*.test.{ts,tsx}',
    ],
    exclude: [
      'node_modules/**',
      'dist/**',
      '.git/**',
      'src/tests/e2e/**',
    ],
    testTimeout: 10000,
    hookTimeout: 10000,
    teardownTimeout: 1000,
    isolate: true,
    watch: false,
    pool: 'threads',
    poolOptions: {
      threads: {
        singleThread: false,
      },
    },
    reporters: ['verbose'],
    outputFile: {
      json: './coverage/test-results.json',
      junit: './coverage/junit.xml',
    },
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      '@components': resolve(__dirname, './src/components'),
      '@pages': resolve(__dirname, './src/pages'),
      '@utils': resolve(__dirname, './src/utils'),
      '@services': resolve(__dirname, './src/services'),
      '@types': resolve(__dirname, './src/types'),
      '@contexts': resolve(__dirname, './src/contexts'),
      '@hooks': resolve(__dirname, './src/hooks'),
    },
  },
})