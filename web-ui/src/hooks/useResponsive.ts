import { useState, useEffect, useMemo } from 'react';

/**
 * Responsive breakpoints following mobile-first approach
 * Matches Tailwind CSS default breakpoints with custom additions
 */
export const BREAKPOINTS = {
  xs: 0,      // Extra small devices (phones, portrait)
  sm: 640,    // Small devices (phones, landscape)
  md: 768,    // Medium devices (tablets, portrait)
  lg: 1024,   // Large devices (tablets, landscape / small laptops)
  xl: 1280,   // Extra large devices (laptops)
  '2xl': 1536, // 2X large devices (large laptops/desktops)
  'laptop-sm': 1366, // Small laptops
  'laptop-lg': 1440, // Large laptops
  'desktop': 1920,   // Desktop displays
} as const;

export type BreakpointKey = keyof typeof BREAKPOINTS;
export type BreakpointValue = typeof BREAKPOINTS[BreakpointKey];

/**
 * Device categories for easier responsive logic
 */
export const DEVICE_CATEGORIES = {
  mobile: { min: 0, max: 767 },
  tablet: { min: 768, max: 1023 },
  laptop: { min: 1024, max: 1439 },
  desktop: { min: 1440, max: Infinity },
} as const;

export type DeviceCategory = keyof typeof DEVICE_CATEGORIES;

/**
 * Screen orientation detection
 */
export type Orientation = 'portrait' | 'landscape';

/**
 * Device pixel ratio categories
 */
export type PixelRatio = 'standard' | 'retina' | 'ultra';

/**
 * Touch capability detection
 */
export type TouchCapability = 'touch' | 'no-touch' | 'hybrid';

/**
 * Comprehensive responsive information
 */
export interface ResponsiveInfo {
  // Current screen width
  width: number;
  
  // Current screen height
  height: number;
  
  // Current breakpoint
  breakpoint: BreakpointKey;
  
  // Device category
  category: DeviceCategory;
  
  // Orientation
  orientation: Orientation;
  
  // Pixel ratio category
  pixelRatio: PixelRatio;
  
  // Touch capability
  touch: TouchCapability;
  
  // Breakpoint queries
  isXs: boolean;
  isSm: boolean;
  isMd: boolean;
  isLg: boolean;
  isXl: boolean;
  is2Xl: boolean;
  
  // Device category queries
  isMobile: boolean;
  isTablet: boolean;
  isLaptop: boolean;
  isDesktop: boolean;
  
  // Size comparisons
  isSmallScreen: boolean;
  isMediumScreen: boolean;
  isLargeScreen: boolean;
  
  // Convenience methods
  atLeast: (breakpoint: BreakpointKey) => boolean;
  atMost: (breakpoint: BreakpointKey) => boolean;
  between: (min: BreakpointKey, max: BreakpointKey) => boolean;
}

/**
 * Get the current breakpoint based on screen width
 */
function getCurrentBreakpoint(width: number): BreakpointKey {
  const sortedBreakpoints = Object.entries(BREAKPOINTS)
    .sort(([, a], [, b]) => b - a); // Sort descending
  
  for (const [key, value] of sortedBreakpoints) {
    if (width >= value) {
      return key as BreakpointKey;
    }
  }
  
  return 'xs';
}

/**
 * Get the current device category based on screen width
 */
function getCurrentCategory(width: number): DeviceCategory {
  for (const [category, { min, max }] of Object.entries(DEVICE_CATEGORIES)) {
    if (width >= min && width <= max) {
      return category as DeviceCategory;
    }
  }
  return 'mobile';
}

/**
 * Detect device pixel ratio category
 */
function getPixelRatioCategory(): PixelRatio {
  const ratio = window.devicePixelRatio || 1;
  if (ratio >= 3) return 'ultra';
  if (ratio >= 2) return 'retina';
  return 'standard';
}

/**
 * Detect touch capability
 */
function getTouchCapability(): TouchCapability {
  // Check for touch events support
  const hasTouch = 'ontouchstart' in window || navigator.maxTouchPoints > 0;
  
  // Check for mouse support (for hybrid devices)
  const hasMouse = window.matchMedia('(pointer: fine)').matches;
  
  if (hasTouch && hasMouse) return 'hybrid';
  if (hasTouch) return 'touch';
  return 'no-touch';
}

/**
 * Custom hook for responsive design utilities
 * Provides comprehensive information about the current viewport and device
 */
export function useResponsive(): ResponsiveInfo {
  const [dimensions, setDimensions] = useState(() => ({
    width: typeof window !== 'undefined' ? window.innerWidth : 1024,
    height: typeof window !== 'undefined' ? window.innerHeight : 768,
  }));

  const [orientation, setOrientation] = useState<Orientation>(() => {
    if (typeof window === 'undefined') return 'landscape';
    return window.innerWidth > window.innerHeight ? 'landscape' : 'portrait';
  });

  const [pixelRatio, setPixelRatio] = useState<PixelRatio>(() => {
    if (typeof window === 'undefined') return 'standard';
    return getPixelRatioCategory();
  });

  const [touch, setTouch] = useState<TouchCapability>(() => {
    if (typeof window === 'undefined') return 'no-touch';
    return getTouchCapability();
  });

  // Update dimensions and orientation on resize
  useEffect(() => {
    if (typeof window === 'undefined') return;

    let timeoutId: NodeJS.Timeout;

    const handleResize = () => {
      // Debounce resize events for performance
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        const newWidth = window.innerWidth;
        const newHeight = window.innerHeight;
        
        setDimensions({ width: newWidth, height: newHeight });
        setOrientation(newWidth > newHeight ? 'landscape' : 'portrait');
      }, 100);
    };

    const handleOrientationChange = () => {
      // Handle orientation change (mobile devices)
      setTimeout(() => {
        const newWidth = window.innerWidth;
        const newHeight = window.innerHeight;
        
        setDimensions({ width: newWidth, height: newHeight });
        setOrientation(newWidth > newHeight ? 'landscape' : 'portrait');
      }, 100); // Small delay to ensure dimensions are updated
    };

    window.addEventListener('resize', handleResize, { passive: true });
    window.addEventListener('orientationchange', handleOrientationChange, { passive: true });

    // Handle pixel ratio changes (zoom, external monitor)
    const pixelRatioMedia = window.matchMedia(`(resolution: ${window.devicePixelRatio}dppx)`);
    const handlePixelRatioChange = () => {
      setPixelRatio(getPixelRatioCategory());
    };
    
    if (pixelRatioMedia.addEventListener) {
      pixelRatioMedia.addEventListener('change', handlePixelRatioChange);
    }

    return () => {
      clearTimeout(timeoutId);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('orientationchange', handleOrientationChange);
      
      if (pixelRatioMedia.removeEventListener) {
        pixelRatioMedia.removeEventListener('change', handlePixelRatioChange);
      }
    };
  }, []);

  // Memoize responsive information to prevent unnecessary re-renders
  const responsiveInfo = useMemo((): ResponsiveInfo => {
    const { width, height } = dimensions;
    const breakpoint = getCurrentBreakpoint(width);
    const category = getCurrentCategory(width);

    // Breakpoint queries
    const isXs = breakpoint === 'xs';
    const isSm = breakpoint === 'sm';
    const isMd = breakpoint === 'md';
    const isLg = breakpoint === 'lg';
    const isXl = breakpoint === 'xl';
    const is2Xl = breakpoint === '2xl';

    // Device category queries
    const isMobile = category === 'mobile';
    const isTablet = category === 'tablet';
    const isLaptop = category === 'laptop';
    const isDesktop = category === 'desktop';

    // Size comparisons
    const isSmallScreen = width < BREAKPOINTS.md;
    const isMediumScreen = width >= BREAKPOINTS.md && width < BREAKPOINTS.xl;
    const isLargeScreen = width >= BREAKPOINTS.xl;

    // Convenience methods
    const atLeast = (bp: BreakpointKey): boolean => width >= BREAKPOINTS[bp];
    const atMost = (bp: BreakpointKey): boolean => width <= BREAKPOINTS[bp];
    const between = (min: BreakpointKey, max: BreakpointKey): boolean => 
      width >= BREAKPOINTS[min] && width <= BREAKPOINTS[max];

    return {
      width,
      height,
      breakpoint,
      category,
      orientation,
      pixelRatio,
      touch,
      isXs,
      isSm,
      isMd,
      isLg,
      isXl,
      is2Xl,
      isMobile,
      isTablet,
      isLaptop,
      isDesktop,
      isSmallScreen,
      isMediumScreen,
      isLargeScreen,
      atLeast,
      atMost,
      between,
    };
  }, [dimensions, orientation, pixelRatio, touch]);

  return responsiveInfo;
}

/**
 * Hook for specific breakpoint queries
 * More performant when you only need specific breakpoint information
 */
export function useBreakpoint(breakpoint: BreakpointKey): boolean {
  const [matches, setMatches] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.innerWidth >= BREAKPOINTS[breakpoint];
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const mediaQuery = window.matchMedia(`(min-width: ${BREAKPOINTS[breakpoint]}px)`);
    
    const handleChange = (e: MediaQueryListEvent) => {
      setMatches(e.matches);
    };

    setMatches(mediaQuery.matches);

    if (mediaQuery.addEventListener) {
      mediaQuery.addEventListener('change', handleChange);
      return () => mediaQuery.removeEventListener('change', handleChange);
    } else {
      // Fallback for older browsers
      mediaQuery.addListener(handleChange);
      return () => mediaQuery.removeListener(handleChange);
    }
  }, [breakpoint]);

  return matches;
}

/**
 * Hook for device category detection
 */
export function useDeviceCategory(): DeviceCategory {
  const { category } = useResponsive();
  return category;
}

/**
 * Hook for orientation detection
 */
export function useOrientation(): Orientation {
  const { orientation } = useResponsive();
  return orientation;
}

/**
 * Hook for touch capability detection
 */
export function useTouchCapability(): TouchCapability {
  const { touch } = useResponsive();
  return touch;
}

/**
 * Hook for detecting if the device is mobile (touch + small screen)
 */
export function useIsMobile(): boolean {
  const { isMobile } = useResponsive();
  return isMobile;
}

/**
 * Hook for safe area insets (for mobile devices with notches, etc.)
 */
export function useSafeAreaInsets() {
  const [insets, setInsets] = useState({
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const updateInsets = () => {
      const computedStyle = getComputedStyle(document.documentElement);
      
      setInsets({
        top: parseInt(computedStyle.getPropertyValue('env(safe-area-inset-top)') || '0'),
        right: parseInt(computedStyle.getPropertyValue('env(safe-area-inset-right)') || '0'),
        bottom: parseInt(computedStyle.getPropertyValue('env(safe-area-inset-bottom)') || '0'),
        left: parseInt(computedStyle.getPropertyValue('env(safe-area-inset-left)') || '0'),
      });
    };

    updateInsets();

    // Update on orientation change
    window.addEventListener('orientationchange', updateInsets);
    window.addEventListener('resize', updateInsets);

    return () => {
      window.removeEventListener('orientationchange', updateInsets);
      window.removeEventListener('resize', updateInsets);
    };
  }, []);

  return insets;
}

/**
 * Type guard to check if a value is a valid breakpoint key
 */
export function isBreakpointKey(value: string): value is BreakpointKey {
  return value in BREAKPOINTS;
}

/**
 * Utility function to get responsive classes based on breakpoint
 */
export function getResponsiveClasses<T extends Record<string, string>>(
  classes: T,
  currentBreakpoint: BreakpointKey
): string {
  const breakpointOrder: BreakpointKey[] = ['xs', 'sm', 'md', 'lg', 'xl', '2xl', 'laptop-sm', 'laptop-lg', 'desktop'];
  const currentIndex = breakpointOrder.indexOf(currentBreakpoint);
  
  let resultClass = '';
  
  for (let i = 0; i <= currentIndex; i++) {
    const bp = breakpointOrder[i];
    if (classes[bp]) {
      resultClass = classes[bp];
    }
  }
  
  return resultClass;
}

/**
 * Export default hook for convenience
 */
export default useResponsive;