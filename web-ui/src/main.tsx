import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'

// Global error handling for unhandled promise rejections
window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason);
  
  // Report to error service if available
  if (window.errorReporting) {
    window.errorReporting.captureException(event.reason);
  }
  
  // Prevent the default browser behavior (logging to console)
  event.preventDefault();
});

// Global error handling for uncaught errors
window.addEventListener('error', (event) => {
  console.error('Uncaught error:', event.error);
  
  // Report to error service if available
  if (window.errorReporting) {
    window.errorReporting.captureException(event.error);
  }
});

// Service Worker Registration
const registerServiceWorker = async () => {
  if ('serviceWorker' in navigator && import.meta.env.PROD) {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js', {
        scope: '/',
      });

      console.log('✅ Service Worker registered successfully:', registration.scope);

      // Listen for updates
      registration.addEventListener('updatefound', () => {
        const newWorker = registration.installing;
        if (newWorker) {
          newWorker.addEventListener('statechange', () => {
            if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
              // New content available, show update notification
              if (window.showUpdateNotification) {
                window.showUpdateNotification();
              } else {
                console.log('🔄 New app version available! Refresh to update.');
              }
            }
          });
        }
      });

      // Listen for service worker messages
      navigator.serviceWorker.addEventListener('message', (event) => {
        const { type, data } = event.data || {};
        
        if (type === 'CACHE_UPDATED') {
          console.log('📦 Cache updated:', data);
        }
      });

    } catch (error) {
      console.error('❌ Service Worker registration failed:', error);
    }
  } else if (!import.meta.env.PROD) {
    console.log('🚧 Service Worker disabled in development mode');
  } else {
    console.log('❌ Service Worker not supported');
  }
};

// Initialize performance monitoring and error reporting
const initializeMonitoring = () => {
  // Web Vitals monitoring (optional)
  if (import.meta.env.PROD) {
    // Use basic performance API if web-vitals is not available
    try {
      // Monitor Core Web Vitals using Performance Observer API
      if ('PerformanceObserver' in window) {
        const observer = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            console.log(`Performance metric: ${entry.name}`, entry);
          }
        });
        
        observer.observe({ entryTypes: ['measure', 'navigation', 'resource'] });
      }
    } catch (error) {
      console.log('Performance monitoring not available:', error);
    }
  }
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

// Initialize after render
registerServiceWorker();
initializeMonitoring();
