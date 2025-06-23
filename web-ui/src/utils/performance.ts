/**
 * Performance monitoring and optimization utilities for AIMS Web Application
 * Provides Core Web Vitals tracking, performance measurements, and optimization helpers
 */

// Performance measurement interfaces
export interface PerformanceMetrics {
  fcp: number | null; // First Contentful Paint
  lcp: number | null; // Largest Contentful Paint
  fid: number | null; // First Input Delay
  cls: number | null; // Cumulative Layout Shift
  ttfb: number | null; // Time to First Byte
  tti: number | null; // Time to Interactive
}

export interface NavigationTiming {
  domContentLoaded: number;
  loadComplete: number;
  firstPaint: number;
  firstContentfulPaint: number;
}

export interface ResourceTiming {
  name: string;
  type: string;
  size: number;
  duration: number;
  startTime: number;
}

// Core Web Vitals thresholds (Google's recommendations)
export const PERFORMANCE_THRESHOLDS = {
  FCP: { good: 1800, needsImprovement: 3000 },
  LCP: { good: 2500, needsImprovement: 4000 },
  FID: { good: 100, needsImprovement: 300 },
  CLS: { good: 0.1, needsImprovement: 0.25 },
  TTFB: { good: 800, needsImprovement: 1800 },
} as const;

// Performance observer for Core Web Vitals
class PerformanceMonitor {
  private metrics: PerformanceMetrics = {
    fcp: null,
    lcp: null,
    fid: null,
    cls: null,
    ttfb: null,
    tti: null,
  };

  private observers: PerformanceObserver[] = [];
  private listeners: ((metrics: PerformanceMetrics) => void)[] = [];

  constructor() {
    this.initializeObservers();
    this.measureTTFB();
  }

  private initializeObservers() {
    // First Contentful Paint & Largest Contentful Paint
    if ('PerformanceObserver' in window) {
      try {
        const paintObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (entry.name === 'first-contentful-paint') {
              this.metrics.fcp = entry.startTime;
            }
          }
          this.notifyListeners();
        });
        paintObserver.observe({ entryTypes: ['paint'] });
        this.observers.push(paintObserver);

        // Largest Contentful Paint
        const lcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries();
          const lastEntry = entries[entries.length - 1];
          this.metrics.lcp = lastEntry.startTime;
          this.notifyListeners();
        });
        lcpObserver.observe({ entryTypes: ['largest-contentful-paint'] });
        this.observers.push(lcpObserver);

        // First Input Delay
        const fidObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            const fidEntry = entry as any; // First Input Delay entries have processingStart
            this.metrics.fid = fidEntry.processingStart - fidEntry.startTime;
          }
          this.notifyListeners();
        });
        fidObserver.observe({ entryTypes: ['first-input'] });
        this.observers.push(fidObserver);

        // Cumulative Layout Shift
        let clsValue = 0;
        const clsObserver = new PerformanceObserver((list) => {
          for (const entry of list.getEntries() as any[]) {
            if (!entry.hadRecentInput) {
              clsValue += entry.value;
            }
          }
          this.metrics.cls = clsValue;
          this.notifyListeners();
        });
        clsObserver.observe({ entryTypes: ['layout-shift'] });
        this.observers.push(clsObserver);
      } catch (error) {
        console.warn('Performance Observer not supported:', error);
      }
    }
  }

  private measureTTFB() {
    if ('performance' in window && 'getEntriesByType' in performance) {
      const navEntries = performance.getEntriesByType('navigation') as PerformanceNavigationTiming[];
      if (navEntries.length > 0) {
        this.metrics.ttfb = navEntries[0].responseStart - navEntries[0].fetchStart;
        this.notifyListeners();
      }
    }
  }

  private notifyListeners() {
    this.listeners.forEach(listener => listener(this.metrics));
  }

  public subscribe(callback: (metrics: PerformanceMetrics) => void) {
    this.listeners.push(callback);
    return () => {
      const index = this.listeners.indexOf(callback);
      if (index > -1) {
        this.listeners.splice(index, 1);
      }
    };
  }

  public getMetrics(): PerformanceMetrics {
    return { ...this.metrics };
  }

  public getNavigationTiming(): NavigationTiming | null {
    if (!('performance' in window)) return null;

    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
    if (!navigation) return null;

    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.fetchStart,
      loadComplete: navigation.loadEventEnd - navigation.fetchStart,
      firstPaint: this.metrics.fcp || 0,
      firstContentfulPaint: this.metrics.fcp || 0,
    };
  }

  public getResourceTiming(): ResourceTiming[] {
    if (!('performance' in window)) return [];

    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
    return resources.map(resource => ({
      name: resource.name,
      type: this.getResourceType(resource.name),
      size: resource.transferSize || 0,
      duration: resource.responseEnd - resource.startTime,
      startTime: resource.startTime,
    }));
  }

  private getResourceType(name: string): string {
    if (name.includes('.js')) return 'script';
    if (name.includes('.css')) return 'stylesheet';
    if (name.match(/\.(png|jpg|jpeg|gif|svg|webp)$/)) return 'image';
    if (name.includes('.woff') || name.includes('.ttf')) return 'font';
    return 'other';
  }

  public measureCustomMetric(name: string, startTime?: number): () => void {
    const start = startTime || performance.now();
    return () => {
      const duration = performance.now() - start;
      performance.mark(`${name}-end`);
      performance.measure(name, { start, end: performance.now() });
      console.log(`Custom metric "${name}": ${duration.toFixed(2)}ms`);
    };
  }

  public reportMetrics(): void {
    const metrics = this.getMetrics();
    const navigation = this.getNavigationTiming();
    
    console.group('🚀 Performance Metrics Report');
    console.log('Core Web Vitals:', {
      FCP: metrics.fcp ? `${metrics.fcp.toFixed(2)}ms` : 'Not measured',
      LCP: metrics.lcp ? `${metrics.lcp.toFixed(2)}ms` : 'Not measured',
      FID: metrics.fid ? `${metrics.fid.toFixed(2)}ms` : 'Not measured',
      CLS: metrics.cls ? metrics.cls.toFixed(4) : 'Not measured',
      TTFB: metrics.ttfb ? `${metrics.ttfb.toFixed(2)}ms` : 'Not measured',
    });
    
    if (navigation) {
      console.log('Navigation Timing:', {
        'DOM Content Loaded': `${navigation.domContentLoaded.toFixed(2)}ms`,
        'Load Complete': `${navigation.loadComplete.toFixed(2)}ms`,
      });
    }

    // Performance scores
    console.log('Performance Scores:', {
      FCP: this.getScore('FCP', metrics.fcp),
      LCP: this.getScore('LCP', metrics.lcp),
      FID: this.getScore('FID', metrics.fid),
      CLS: this.getScore('CLS', metrics.cls),
    });
    console.groupEnd();
  }

  private getScore(metric: keyof typeof PERFORMANCE_THRESHOLDS, value: number | null): string {
    if (value === null) return 'Not measured';
    
    const thresholds = PERFORMANCE_THRESHOLDS[metric];
    if (value <= thresholds.good) return '🟢 Good';
    if (value <= thresholds.needsImprovement) return '🟡 Needs Improvement';
    return '🔴 Poor';
  }

  public disconnect() {
    this.observers.forEach(observer => observer.disconnect());
    this.observers = [];
    this.listeners = [];
  }
}

// Global performance monitor instance
let performanceMonitor: PerformanceMonitor | null = null;

// Utility functions
export function initializePerformanceMonitoring(): PerformanceMonitor {
  if (!performanceMonitor) {
    performanceMonitor = new PerformanceMonitor();
  }
  return performanceMonitor;
}

export function getPerformanceMonitor(): PerformanceMonitor | null {
  return performanceMonitor;
}

// React Hook for performance monitoring
export function usePerformanceMetrics() {
  const [metrics, setMetrics] = React.useState<PerformanceMetrics>({
    fcp: null,
    lcp: null,
    fid: null,
    cls: null,
    ttfb: null,
    tti: null,
  });

  React.useEffect(() => {
    const monitor = initializePerformanceMonitoring();
    const unsubscribe = monitor.subscribe(setMetrics);
    
    return () => {
      unsubscribe();
    };
  }, []);

  return metrics;
}

// Performance measurement decorator
export function measurePerformance(target: any, propertyName: string, descriptor: PropertyDescriptor) {
  const method = descriptor.value;

  descriptor.value = function (...args: any[]) {
    const start = performance.now();
    const result = method.apply(this, args);

    if (result instanceof Promise) {
      return result.finally(() => {
        const duration = performance.now() - start;
        console.log(`⏱️ ${target.constructor.name}.${propertyName}: ${duration.toFixed(2)}ms`);
      });
    } else {
      const duration = performance.now() - start;
      console.log(`⏱️ ${target.constructor.name}.${propertyName}: ${duration.toFixed(2)}ms`);
      return result;
    }
  };

  return descriptor;
}

// Bundle size monitoring
export function logBundleInfo() {
  if ('performance' in window) {
    const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
    const jsResources = resources.filter(r => r.name.includes('.js'));
    const cssResources = resources.filter(r => r.name.includes('.css'));
    
    console.group('📦 Bundle Size Information');
    console.log('JavaScript files:', jsResources.map(r => ({
      name: r.name.split('/').pop(),
      size: `${(r.transferSize / 1024).toFixed(1)}KB`,
      gzipped: `${(r.encodedBodySize / 1024).toFixed(1)}KB`,
    })));
    console.log('CSS files:', cssResources.map(r => ({
      name: r.name.split('/').pop(),
      size: `${(r.transferSize / 1024).toFixed(1)}KB`,
    })));
    console.groupEnd();
  }
}

// Memory usage monitoring
export function logMemoryUsage() {
  if ('memory' in performance) {
    const memory = (performance as any).memory;
    console.group('💾 Memory Usage');
    console.log('Used:', `${(memory.usedJSHeapSize / 1024 / 1024).toFixed(1)}MB`);
    console.log('Total:', `${(memory.totalJSHeapSize / 1024 / 1024).toFixed(1)}MB`);
    console.log('Limit:', `${(memory.jsHeapSizeLimit / 1024 / 1024).toFixed(1)}MB`);
    console.groupEnd();
  }
}

// Auto-initialize in development
if (import.meta.env.DEV) {
  window.addEventListener('load', () => {
    const monitor = initializePerformanceMonitoring();
    setTimeout(() => monitor.reportMetrics(), 3000); // Report after 3 seconds
    setTimeout(() => logBundleInfo(), 1000);
    setTimeout(() => logMemoryUsage(), 1000);
  });
}

// Add React import for hook
import React from 'react';