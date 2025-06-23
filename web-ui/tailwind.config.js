/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
          950: '#172554'
        },
        success: '#10b981',
        warning: '#f59e0b',
        error: '#ef4444',
        info: '#6366f1',
        // WCAG AA compliant color variants
        'contrast-aa': {
          'text': '#1f2937',      // 4.5:1 ratio on white
          'text-large': '#4b5563', // 3:1 ratio on white for large text
          'error': '#b91c1c',     // 4.5:1 ratio on white
          'success': '#065f46',   // 4.5:1 ratio on white
          'warning': '#92400e',   // 4.5:1 ratio on white
        }
      },
      screens: {
        'xs': '475px',        // Extra small devices
        'laptop-sm': '1366px', // Small laptops
        'laptop-lg': '1440px', // Large laptops
        'desktop': '1920px',   // Desktop displays
        // Touch-specific breakpoints
        'touch': { 'raw': '(hover: none) and (pointer: coarse)' },
        'no-touch': { 'raw': '(hover: hover) and (pointer: fine)' },
        // Accessibility preference breakpoints
        'reduce-motion': { 'raw': '(prefers-reduced-motion: reduce)' },
        'high-contrast': { 'raw': '(prefers-contrast: high)' },
        'dark': { 'raw': '(prefers-color-scheme: dark)' },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      spacing: {
        // Touch-friendly spacing
        'touch': '44px', // Minimum touch target size
      },
      fontSize: {
        // Accessible font sizes
        'a11y-xs': ['0.75rem', { lineHeight: '1.5' }],
        'a11y-sm': ['0.875rem', { lineHeight: '1.5' }],
        'a11y-base': ['1rem', { lineHeight: '1.5' }],
        'a11y-lg': ['1.125rem', { lineHeight: '1.5' }],
        'a11y-xl': ['1.25rem', { lineHeight: '1.5' }],
      },
      boxShadow: {
        'focus': '0 0 0 2px #3b82f6, 0 0 0 4px rgba(59, 130, 246, 0.1)',
        'focus-error': '0 0 0 2px #ef4444, 0 0 0 4px rgba(239, 68, 68, 0.1)',
        'focus-success': '0 0 0 2px #10b981, 0 0 0 4px rgba(16, 185, 129, 0.1)',
      },
      animation: {
        'fade-in': 'fadeIn 0.2s ease-in-out',
        'slide-in': 'slideIn 0.3s ease-out',
        'scale-in': 'scaleIn 0.2s ease-out',
        // Reduced motion alternatives
        'fade-in-reduced': 'fadeInReduced 0.01ms',
        'slide-in-reduced': 'slideInReduced 0.01ms',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideIn: {
          '0%': { transform: 'translateY(-10px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' }
        },
        scaleIn: {
          '0%': { transform: 'scale(0.95)', opacity: '0' },
          '100%': { transform: 'scale(1)', opacity: '1' }
        },
        fadeInReduced: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        },
        slideInReduced: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' }
        }
      },
      // Safe area utilities for mobile devices
      padding: {
        'safe-top': 'max(1rem, env(safe-area-inset-top))',
        'safe-bottom': 'max(1rem, env(safe-area-inset-bottom))',
        'safe-left': 'max(1rem, env(safe-area-inset-left))',
        'safe-right': 'max(1rem, env(safe-area-inset-right))',
      },
      margin: {
        'safe-top': 'max(1rem, env(safe-area-inset-top))',
        'safe-bottom': 'max(1rem, env(safe-area-inset-bottom))',
        'safe-left': 'max(1rem, env(safe-area-inset-left))',
        'safe-right': 'max(1rem, env(safe-area-inset-right))',
      }
    },
  },
  plugins: [
    // Keep it simple for v4 compatibility
  ],
}