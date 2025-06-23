// Core hooks
export * from './useProducts';
export * from './useProductFilters';
export * from './useCart';
export * from './useNetworkStatus';

// Accessibility hooks
export {
  default as useAccessibility,
  useFocusManagement,
  useScreenReaderAnnouncements,
  useKeyboardNavigation,
  useReducedMotion,
  useHighContrast,
  useAriaUtilities,
} from './useAccessibility';

export type {
  AccessibilityPreferences,
  FocusManagement,
  AnnouncementUtilities,
  AriaUtilities,
  KeyboardNavigation,
  AccessibilityUtilities,
} from './useAccessibility';

// Responsive design hooks
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
} from './useResponsive';

export type {
  ResponsiveInfo,
  BreakpointKey,
  BreakpointValue,
  DeviceCategory,
  Orientation,
  PixelRatio,
  TouchCapability,
} from './useResponsive';

export { BREAKPOINTS, DEVICE_CATEGORIES } from './useResponsive';