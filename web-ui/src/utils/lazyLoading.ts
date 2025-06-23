/**
 * Lazy loading utilities for AIMS Web Application
 * Provides dynamic imports, lazy loading helpers, and preloading strategies
 */

import React from 'react';
import { lazy } from 'react';
import type { ComponentType, LazyExoticComponent } from 'react';

// Types for lazy loading
export interface LazyLoadOptions {
  fallback?: ComponentType;
  preload?: boolean;
  timeout?: number;
  retries?: number;
}

export interface LazyComponentMap {
  [key: string]: () => Promise<{ default: ComponentType<any> }>;
}

// Lazy loading helper with enhanced error handling and preloading
export function createLazyComponent<T extends ComponentType<any>>(
  importFunction: () => Promise<{ default: T }>,
  options: LazyLoadOptions = {}
): LazyExoticComponent<T> {
  const { timeout = 10000, retries = 3 } = options;

  const enhancedImport = async (): Promise<{ default: T }> => {
    let lastError: Error | undefined;
    
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        const timeoutPromise = new Promise<never>((_, reject) => {
          setTimeout(() => reject(new Error('Import timeout')), timeout);
        });

        const result = await Promise.race([importFunction(), timeoutPromise]);
        
        // Validate the imported component
        if (!result || !result.default) {
          throw new Error('Invalid component export');
        }
        
        return result;
      } catch (error) {
        lastError = error as Error;
        console.warn(`Lazy load attempt ${attempt} failed:`, error);
        
        if (attempt < retries) {
          // Exponential backoff
          await new Promise(resolve => setTimeout(resolve, Math.pow(2, attempt) * 1000));
        }
      }
    }
    
    throw new Error(`Failed to load component after ${retries} attempts: ${lastError?.message || 'Unknown error'}`);
  };

  return lazy(enhancedImport);
}

// Preload components for better user experience
export function preloadComponent(importFunction: () => Promise<any>): Promise<void> {
  return importFunction()
    .then(() => {
      console.log('Component preloaded successfully');
    })
    .catch((error) => {
      console.warn('Failed to preload component:', error);
    });
}

// Lazy load multiple components with dependency management
export function createLazyComponentMap(
  componentMap: LazyComponentMap,
  options: LazyLoadOptions = {}
): Record<string, LazyExoticComponent<any>> {
  const lazyComponents: Record<string, LazyExoticComponent<any>> = {};
  
  Object.entries(componentMap).forEach(([key, importFunction]) => {
    lazyComponents[key] = createLazyComponent(importFunction, options);
  });
  
  return lazyComponents;
}

// Route-based preloading strategy
export class RoutePreloader {
  private preloadedRoutes = new Set<string>();
  private preloadQueue: Array<() => Promise<any>> = [];
  private isProcessing = false;
  private maxConcurrent: number;

  constructor(maxConcurrent = 2) {
    this.maxConcurrent = maxConcurrent;
  }

  addRoute(routePath: string, importFunction: () => Promise<any>) {
    if (this.preloadedRoutes.has(routePath)) {
      return;
    }

    this.preloadQueue.push(async () => {
      try {
        await importFunction();
        this.preloadedRoutes.add(routePath);
        console.log(`Route preloaded: ${routePath}`);
      } catch (error) {
        console.warn(`Failed to preload route ${routePath}:`, error);
      }
    });

    this.processQueue();
  }

  private async processQueue() {
    if (this.isProcessing || this.preloadQueue.length === 0) {
      return;
    }

    this.isProcessing = true;
    const concurrent = this.preloadQueue.splice(0, this.maxConcurrent);
    
    try {
      await Promise.all(concurrent.map(preload => preload()));
    } catch (error) {
      console.warn('Error in preload queue processing:', error);
    }

    this.isProcessing = false;
    
    // Process remaining queue
    if (this.preloadQueue.length > 0) {
      setTimeout(() => this.processQueue(), 100);
    }
  }

  isPreloaded(routePath: string): boolean {
    return this.preloadedRoutes.has(routePath);
  }

  preloadOnHover(routePath: string, importFunction: () => Promise<any>) {
    return {
      onMouseEnter: () => this.addRoute(routePath, importFunction),
      onFocus: () => this.addRoute(routePath, importFunction),
    };
  }
}

// Global route preloader instance
export const routePreloader = new RoutePreloader();

// Intersection Observer for lazy loading images and components
export class LazyImageLoader {
  private observer: IntersectionObserver;
  private loadedImages = new Set<string>();

  constructor(options: IntersectionObserverInit = {}) {
    this.observer = new IntersectionObserver(
      this.handleIntersection.bind(this),
      {
        rootMargin: '50px',
        threshold: 0.1,
        ...options,
      }
    );
  }

  private handleIntersection(entries: IntersectionObserverEntry[]) {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const img = entry.target as HTMLImageElement;
        const src = img.dataset.src;
        
        if (src && !this.loadedImages.has(src)) {
          this.loadImage(img, src);
          this.observer.unobserve(img);
        }
      }
    });
  }

  private async loadImage(img: HTMLImageElement, src: string) {
    try {
      // Create a new image to test loading
      const testImg = new Image();
      
      await new Promise((resolve, reject) => {
        testImg.onload = resolve;
        testImg.onerror = reject;
        testImg.src = src;
      });

      // If successful, update the actual image
      img.src = src;
      img.classList.remove('lazy-loading');
      img.classList.add('lazy-loaded');
      this.loadedImages.add(src);
      
    } catch (error) {
      console.warn(`Failed to load image: ${src}`, error);
      img.classList.add('lazy-error');
    }
  }

  observe(element: HTMLImageElement) {
    this.observer.observe(element);
  }

  unobserve(element: HTMLImageElement) {
    this.observer.unobserve(element);
  }

  disconnect() {
    this.observer.disconnect();
    this.loadedImages.clear();
  }
}

// React hook for lazy image loading
export function useLazyImageLoader(options?: IntersectionObserverInit) {
  const [loader] = React.useState(() => new LazyImageLoader(options));
  
  React.useEffect(() => {
    return () => loader.disconnect();
  }, [loader]);

  const observeImage = React.useCallback((element: HTMLImageElement | null) => {
    if (element) {
      loader.observe(element);
    }
  }, [loader]);

  return { observeImage, loader };
}

// Utility for preloading critical resources
export class ResourcePreloader {
  private preloadedResources = new Set<string>();

  preloadScript(src: string, integrity?: string): Promise<void> {
    if (this.preloadedResources.has(src)) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'script';
      link.href = src;
      if (integrity) link.integrity = integrity;
      
      link.onload = () => {
        this.preloadedResources.add(src);
        resolve();
      };
      link.onerror = reject;
      
      document.head.appendChild(link);
    });
  }

  preloadStylesheet(href: string, integrity?: string): Promise<void> {
    if (this.preloadedResources.has(href)) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'style';
      link.href = href;
      if (integrity) link.integrity = integrity;
      
      link.onload = () => {
        this.preloadedResources.add(href);
        resolve();
      };
      link.onerror = reject;
      
      document.head.appendChild(link);
    });
  }

  preloadImage(src: string): Promise<void> {
    if (this.preloadedResources.has(src)) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        this.preloadedResources.add(src);
        resolve();
      };
      img.onerror = reject;
      img.src = src;
    });
  }

  preloadFont(href: string, type = 'font/woff2'): Promise<void> {
    if (this.preloadedResources.has(href)) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const link = document.createElement('link');
      link.rel = 'preload';
      link.as = 'font';
      link.type = type;
      link.href = href;
      link.crossOrigin = 'anonymous';
      
      link.onload = () => {
        this.preloadedResources.add(href);
        resolve();
      };
      link.onerror = reject;
      
      document.head.appendChild(link);
    });
  }
}

// Global resource preloader
export const resourcePreloader = new ResourcePreloader();

// Bundle splitting helpers
export const bundlePreloader = {
  preloadVendorChunk: () => {
    if (import.meta.env.PROD) {
      // In production, preload vendor chunks
      return Promise.all([
        import('@tanstack/react-query'),
        import('axios'),
        import('zustand'),
      ]).catch(console.warn);
    }
    return Promise.resolve();
  },

  preloadUIChunk: () => {
    if (import.meta.env.PROD) {
      return Promise.all([
        import('@headlessui/react'),
        import('framer-motion'),
      ]).catch(console.warn);
    }
    return Promise.resolve();
  },

  preloadRouteChunk: (routeName: 'products' | 'cart' | 'checkout' | 'orders' | 'admin') => {
    const routeChunks = {
      products: () => Promise.all([
        import('../pages/ProductListPage'),
        import('../pages/ProductDetailPage'),
      ]),
      cart: () => import('../pages/CartPage'),
      checkout: () => import('../pages/CheckoutPage'),
      orders: () => Promise.all([
        import('../pages/OrdersPage'),
        import('../pages/OrderDetailPage'),
      ]),
      admin: () => import('../pages/admin/AdminDashboard'),
    };

    return routeChunks[routeName]?.().catch(console.warn);
  },
};