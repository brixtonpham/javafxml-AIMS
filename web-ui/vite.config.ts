/// <reference types="vitest" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
  build: {
    target: 'es2020',
    outDir: 'dist',
    sourcemap: false,
    cssCodeSplit: true,
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
      },
    },
    rollupOptions: {
      output: {
        manualChunks: {
          // Vendor chunk for React and core libraries
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          // UI libraries chunk
          'vendor-ui': ['@headlessui/react', '@heroicons/react', 'framer-motion'],
          // Utility libraries chunk
          'vendor-utils': ['axios', '@tanstack/react-query', 'zod', 'zustand'],
          // Form libraries chunk
          'vendor-forms': ['react-hook-form', '@hookform/resolvers'],
          // Chart for each major page section
          'pages-products': [
            './src/pages/ProductListPage.tsx',
            './src/pages/ProductDetailPage.tsx',
          ],
          'pages-cart': [
            './src/pages/CartPage.tsx',
          ],
          'pages-checkout': [
            './src/pages/CheckoutPage.tsx',
          ],
          'pages-orders': [
            './src/pages/OrdersPage.tsx',
            './src/pages/OrderDetailPage.tsx',
          ],
          'pages-payment': [
            './src/pages/PaymentProcessing.tsx',
            './src/pages/PaymentResult.tsx',
          ],
          'pages-admin': [
            './src/pages/admin/AdminDashboard.tsx',
          ],
        },
        chunkFileNames: (chunkInfo) => {
          const facadeModuleId = chunkInfo.facadeModuleId ? chunkInfo.facadeModuleId.split('/').pop() : 'chunk';
          return `js/${chunkInfo.name || facadeModuleId}-[hash].js`;
        },
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name?.endsWith('.css')) {
            return 'css/[name]-[hash][extname]';
          }
          if (/\.(png|jpe?g|svg|gif|tiff|bmp|ico)$/i.test(assetInfo.name || '')) {
            return 'img/[name]-[hash][extname]';
          }
          return 'assets/[name]-[hash][extname]';
        },
      },
    },
    chunkSizeWarningLimit: 1000,
  },
  server: {
    port: 3000,
    host: true,
    cors: true,
    proxy: {
      // Proxy API requests to backend server
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        timeout: 10000,
        rewrite: (path) => path, // Don't rewrite the path, keep /api
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, _res) => {
            console.log('🚨 Proxy error:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, _res) => {
            const host = proxyReq.getHeader('host') ?? 'localhost:8080';
            const url = req.url ?? '/';
            console.log('🔄 Proxying request:', req.method, url, '→', host + url);
          });
          proxy.on('proxyRes', (proxyRes, req, _res) => {
            console.log('✅ Proxy response:', proxyRes.statusCode, req.url ?? '/');
          });
        },
      },
    },
  },
  preview: {
    port: 3000,
    host: true,
  },
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
      '@tanstack/react-query',
      'axios',
      'zustand',
    ],
  },
})
