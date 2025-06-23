/**
 * Accessibility Components Library
 * 
 * A comprehensive collection of accessibility-focused React components
 * that help implement WCAG 2.1 AA compliance in the AIMS application.
 */

// Core accessibility components
export { default as ScreenReaderOnly, useScreenReaderOnly } from './ScreenReaderOnly';
export type { ScreenReaderOnlyProps } from './ScreenReaderOnly';

export { default as FocusTrap } from './FocusTrap';
export type { FocusTrapProps } from './FocusTrap';

export { default as SkipLink, SkipLinks, CommonSkipLinks } from './SkipLink';
export type { SkipLinkProps, SkipLinksProps } from './SkipLink';

// Re-export accessibility hooks for convenience
export {
  default as useAccessibility,
  useFocusManagement,
  useScreenReaderAnnouncements,
  useKeyboardNavigation,
  useReducedMotion,
  useHighContrast,
  useAriaUtilities,
} from '../../hooks/useAccessibility';

export type {
  AccessibilityPreferences,
  FocusManagement,
  AnnouncementUtilities,
  AriaUtilities,
  KeyboardNavigation,
  AccessibilityUtilities,
} from '../../hooks/useAccessibility';

// Re-export responsive hooks for convenience
export {
  default as useResponsive,
  useBreakpoint,
  useDeviceCategory,
  useOrientation,
  useTouchCapability,
  useIsMobile,
  useSafeAreaInsets,
  getResponsiveClasses,
  isBreakpointKey,
} from '../../hooks/useResponsive';

export type {
  ResponsiveInfo,
  BreakpointKey,
  BreakpointValue,
  DeviceCategory,
  Orientation,
  PixelRatio,
  TouchCapability,
} from '../../hooks/useResponsive';

export { BREAKPOINTS, DEVICE_CATEGORIES } from '../../hooks/useResponsive';