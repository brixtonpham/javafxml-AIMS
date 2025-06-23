import { useState, useEffect, useCallback, useRef } from 'react';

/**
 * Accessibility preferences that can be detected or configured
 */
export interface AccessibilityPreferences {
  reduceMotion: boolean;
  highContrast: boolean;
  largeText: boolean;
  darkMode: boolean;
  screenReader: boolean;
  keyboardNavigation: boolean;
  focusVisible: boolean;
}

/**
 * Focus management utilities
 */
export interface FocusManagement {
  trapFocus: (element: HTMLElement) => () => void;
  restoreFocus: (element?: HTMLElement) => void;
  skipToContent: (contentId: string) => void;
  focusElement: (selector: string) => boolean;
  getFocusableElements: (container: HTMLElement) => HTMLElement[];
}

/**
 * Announcement utilities for screen readers
 */
export interface AnnouncementUtilities {
  announce: (message: string, priority?: 'polite' | 'assertive') => void;
  announceSuccess: (message: string) => void;
  announceError: (message: string) => void;
  announceWarning: (message: string) => void;
  announceInfo: (message: string) => void;
  clearAnnouncements: () => void;
}

/**
 * ARIA utilities for better accessibility
 */
export interface AriaUtilities {
  generateId: (prefix?: string) => string;
  createAriaRelationship: (elementId: string, relatedId: string, relationship: string) => void;
  updateAriaLive: (elementId: string, message: string) => void;
  setAriaExpanded: (elementId: string, expanded: boolean) => void;
  setAriaSelected: (elementId: string, selected: boolean) => void;
  setAriaChecked: (elementId: string, checked: boolean | 'mixed') => void;
}

/**
 * Keyboard navigation utilities
 */
export interface KeyboardNavigation {
  handleArrowNavigation: (event: KeyboardEvent, items: HTMLElement[], currentIndex: number) => number;
  handleTabNavigation: (event: KeyboardEvent, container: HTMLElement) => void;
  handleEscapeKey: (event: KeyboardEvent, callback: () => void) => void;
  handleEnterSpace: (event: KeyboardEvent, callback: () => void) => void;
}

/**
 * Complete accessibility utilities interface
 */
export interface AccessibilityUtilities extends AnnouncementUtilities, AriaUtilities, KeyboardNavigation {
  preferences: AccessibilityPreferences;
  focus: FocusManagement;
  updatePreference: <K extends keyof AccessibilityPreferences>(
    key: K,
    value: AccessibilityPreferences[K]
  ) => void;
}

/**
 * Detect user's accessibility preferences from system/browser
 */
function detectAccessibilityPreferences(): AccessibilityPreferences {
  if (typeof window === 'undefined') {
    return {
      reduceMotion: false,
      highContrast: false,
      largeText: false,
      darkMode: false,
      screenReader: false,
      keyboardNavigation: false,
      focusVisible: false,
    };
  }

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const highContrast = window.matchMedia('(prefers-contrast: high)').matches;
  const darkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  // Detect if user is likely using a screen reader
  const screenReader = !!(
    window.navigator.userAgent.match(/NVDA|JAWS|VoiceOver|TalkBack/i) ||
    window.speechSynthesis?.getVoices().length > 0
  );

  // Detect if user prefers keyboard navigation
  const keyboardNavigation = !window.matchMedia('(pointer: fine)').matches;

  return {
    reduceMotion,
    highContrast,
    largeText: false, // Will be detected from user settings or font size
    darkMode,
    screenReader,
    keyboardNavigation,
    focusVisible: true, // Default to true for accessibility
  };
}

/**
 * Create live region for screen reader announcements
 */
function createLiveRegion(priority: 'polite' | 'assertive' = 'polite'): HTMLElement {
  const liveRegion = document.createElement('div');
  liveRegion.setAttribute('aria-live', priority);
  liveRegion.setAttribute('aria-atomic', 'true');
  liveRegion.className = 'sr-only';
  liveRegion.style.cssText = `
    position: absolute !important;
    left: -10000px !important;
    width: 1px !important;
    height: 1px !important;
    overflow: hidden !important;
  `;
  document.body.appendChild(liveRegion);
  return liveRegion;
}

/**
 * Get all focusable elements within a container
 */
function getFocusableElements(container: HTMLElement): HTMLElement[] {
  const focusableSelectors = [
    'a[href]',
    'button:not([disabled])',
    'textarea:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
    '[contenteditable="true"]',
  ].join(', ');

  return Array.from(container.querySelectorAll(focusableSelectors)) as HTMLElement[];
}

/**
 * Main accessibility hook
 */
export function useAccessibility(): AccessibilityUtilities {
  const [preferences, setPreferences] = useState<AccessibilityPreferences>(() => 
    detectAccessibilityPreferences()
  );

  const politeRegionRef = useRef<HTMLElement | null>(null);
  const assertiveRegionRef = useRef<HTMLElement | null>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const idCounterRef = useRef(0);

  // Initialize live regions
  useEffect(() => {
    if (typeof window === 'undefined') return;

    politeRegionRef.current = createLiveRegion('polite');
    assertiveRegionRef.current = createLiveRegion('assertive');

    return () => {
      politeRegionRef.current?.remove();
      assertiveRegionRef.current?.remove();
    };
  }, []);

  // Listen for preference changes
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const mediaQueries = [
      { query: '(prefers-reduced-motion: reduce)', key: 'reduceMotion' as const },
      { query: '(prefers-contrast: high)', key: 'highContrast' as const },
      { query: '(prefers-color-scheme: dark)', key: 'darkMode' as const },
    ];

    const listeners: Array<() => void> = [];

    mediaQueries.forEach(({ query, key }) => {
      const mediaQuery = window.matchMedia(query);
      
      const handleChange = (e: MediaQueryListEvent) => {
        setPreferences(prev => ({ ...prev, [key]: e.matches }));
      };

      if (mediaQuery.addEventListener) {
        mediaQuery.addEventListener('change', handleChange);
        listeners.push(() => mediaQuery.removeEventListener('change', handleChange));
      } else {
        mediaQuery.addListener(handleChange);
        listeners.push(() => mediaQuery.removeListener(handleChange));
      }
    });

    return () => {
      listeners.forEach(cleanup => cleanup());
    };
  }, []);

  // Update preference function
  const updatePreference = useCallback(<K extends keyof AccessibilityPreferences>(
    key: K,
    value: AccessibilityPreferences[K]
  ) => {
    setPreferences(prev => ({ ...prev, [key]: value }));
  }, []);

  // Announcement utilities
  const announce = useCallback((message: string, priority: 'polite' | 'assertive' = 'polite') => {
    const region = priority === 'assertive' ? assertiveRegionRef.current : politeRegionRef.current;
    if (region) {
      region.textContent = message;
      // Clear after a short delay to allow for re-announcements
      setTimeout(() => {
        region.textContent = '';
      }, 1000);
    }
  }, []);

  const announceSuccess = useCallback((message: string) => {
    announce(`Success: ${message}`, 'polite');
  }, [announce]);

  const announceError = useCallback((message: string) => {
    announce(`Error: ${message}`, 'assertive');
  }, [announce]);

  const announceWarning = useCallback((message: string) => {
    announce(`Warning: ${message}`, 'assertive');
  }, [announce]);

  const announceInfo = useCallback((message: string) => {
    announce(`Information: ${message}`, 'polite');
  }, [announce]);

  const clearAnnouncements = useCallback(() => {
    if (politeRegionRef.current) politeRegionRef.current.textContent = '';
    if (assertiveRegionRef.current) assertiveRegionRef.current.textContent = '';
  }, []);

  // ARIA utilities
  const generateId = useCallback((prefix = 'a11y') => {
    return `${prefix}-${++idCounterRef.current}`;
  }, []);

  const createAriaRelationship = useCallback((elementId: string, relatedId: string, relationship: string) => {
    const element = document.getElementById(elementId);
    if (element) {
      element.setAttribute(`aria-${relationship}`, relatedId);
    }
  }, []);

  const updateAriaLive = useCallback((elementId: string, message: string) => {
    const element = document.getElementById(elementId);
    if (element) {
      element.textContent = message;
    }
  }, []);

  const setAriaExpanded = useCallback((elementId: string, expanded: boolean) => {
    const element = document.getElementById(elementId);
    if (element) {
      element.setAttribute('aria-expanded', expanded.toString());
    }
  }, []);

  const setAriaSelected = useCallback((elementId: string, selected: boolean) => {
    const element = document.getElementById(elementId);
    if (element) {
      element.setAttribute('aria-selected', selected.toString());
    }
  }, []);

  const setAriaChecked = useCallback((elementId: string, checked: boolean | 'mixed') => {
    const element = document.getElementById(elementId);
    if (element) {
      element.setAttribute('aria-checked', checked.toString());
    }
  }, []);

  // Focus management utilities
  const trapFocus = useCallback((element: HTMLElement) => {
    const focusableElements = getFocusableElements(element);
    const firstFocusable = focusableElements[0];
    const lastFocusable = focusableElements[focusableElements.length - 1];

    // Store the previously focused element
    previousFocusRef.current = document.activeElement as HTMLElement;

    // Focus the first element
    firstFocusable?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Tab') return;

      if (event.shiftKey) {
        // Shift + Tab
        if (document.activeElement === firstFocusable) {
          event.preventDefault();
          lastFocusable?.focus();
        }
      } else {
        // Tab
        if (document.activeElement === lastFocusable) {
          event.preventDefault();
          firstFocusable?.focus();
        }
      }
    };

    element.addEventListener('keydown', handleKeyDown);

    // Return cleanup function
    return () => {
      element.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const restoreFocus = useCallback((element?: HTMLElement) => {
    const focusTarget = element || previousFocusRef.current;
    if (focusTarget && document.contains(focusTarget)) {
      focusTarget.focus();
    }
  }, []);

  const skipToContent = useCallback((contentId: string) => {
    const content = document.getElementById(contentId);
    if (content) {
      content.focus();
      content.scrollIntoView({ behavior: preferences.reduceMotion ? 'auto' : 'smooth' });
    }
  }, [preferences.reduceMotion]);

  const focusElement = useCallback((selector: string): boolean => {
    const element = document.querySelector(selector) as HTMLElement;
    if (element) {
      element.focus();
      return true;
    }
    return false;
  }, []);

  // Keyboard navigation utilities
  const handleArrowNavigation = useCallback((
    event: KeyboardEvent,
    items: HTMLElement[],
    currentIndex: number
  ): number => {
    let newIndex = currentIndex;

    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowRight':
        event.preventDefault();
        newIndex = (currentIndex + 1) % items.length;
        break;
      case 'ArrowUp':
      case 'ArrowLeft':
        event.preventDefault();
        newIndex = currentIndex <= 0 ? items.length - 1 : currentIndex - 1;
        break;
      case 'Home':
        event.preventDefault();
        newIndex = 0;
        break;
      case 'End':
        event.preventDefault();
        newIndex = items.length - 1;
        break;
    }

    if (newIndex !== currentIndex && items[newIndex]) {
      items[newIndex].focus();
    }

    return newIndex;
  }, []);

  const handleTabNavigation = useCallback((event: KeyboardEvent, container: HTMLElement) => {
    if (event.key !== 'Tab') return;

    const focusableElements = getFocusableElements(container);
    const currentIndex = focusableElements.indexOf(document.activeElement as HTMLElement);

    if (event.shiftKey) {
      // Shift + Tab (backward)
      if (currentIndex <= 0) {
        event.preventDefault();
        focusableElements[focusableElements.length - 1]?.focus();
      }
    } else {
      // Tab (forward)
      if (currentIndex >= focusableElements.length - 1) {
        event.preventDefault();
        focusableElements[0]?.focus();
      }
    }
  }, []);

  const handleEscapeKey = useCallback((event: KeyboardEvent, callback: () => void) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      callback();
    }
  }, []);

  const handleEnterSpace = useCallback((event: KeyboardEvent, callback: () => void) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      callback();
    }
  }, []);

  // Focus management object
  const focus: FocusManagement = {
    trapFocus,
    restoreFocus,
    skipToContent,
    focusElement,
    getFocusableElements,
  };

  return {
    preferences,
    focus,
    updatePreference,
    announce,
    announceSuccess,
    announceError,
    announceWarning,
    announceInfo,
    clearAnnouncements,
    generateId,
    createAriaRelationship,
    updateAriaLive,
    setAriaExpanded,
    setAriaSelected,
    setAriaChecked,
    handleArrowNavigation,
    handleTabNavigation,
    handleEscapeKey,
    handleEnterSpace,
  };
}

/**
 * Hook for focus management only
 */
export function useFocusManagement(): FocusManagement {
  const { focus } = useAccessibility();
  return focus;
}

/**
 * Hook for screen reader announcements only
 */
export function useScreenReaderAnnouncements(): AnnouncementUtilities {
  const {
    announce,
    announceSuccess,
    announceError,
    announceWarning,
    announceInfo,
    clearAnnouncements,
  } = useAccessibility();

  return {
    announce,
    announceSuccess,
    announceError,
    announceWarning,
    announceInfo,
    clearAnnouncements,
  };
}

/**
 * Hook for keyboard navigation only
 */
export function useKeyboardNavigation(): KeyboardNavigation {
  const {
    handleArrowNavigation,
    handleTabNavigation,
    handleEscapeKey,
    handleEnterSpace,
  } = useAccessibility();

  return {
    handleArrowNavigation,
    handleTabNavigation,
    handleEscapeKey,
    handleEnterSpace,
  };
}

/**
 * Hook to check if user prefers reduced motion
 */
export function useReducedMotion(): boolean {
  const { preferences } = useAccessibility();
  return preferences.reduceMotion;
}

/**
 * Hook to check if user prefers high contrast
 */
export function useHighContrast(): boolean {
  const { preferences } = useAccessibility();
  return preferences.highContrast;
}

/**
 * Hook for ARIA utilities only
 */
export function useAriaUtilities(): AriaUtilities {
  const {
    generateId,
    createAriaRelationship,
    updateAriaLive,
    setAriaExpanded,
    setAriaSelected,
    setAriaChecked,
  } = useAccessibility();

  return {
    generateId,
    createAriaRelationship,
    updateAriaLive,
    setAriaExpanded,
    setAriaSelected,
    setAriaChecked,
  };
}

export default useAccessibility;