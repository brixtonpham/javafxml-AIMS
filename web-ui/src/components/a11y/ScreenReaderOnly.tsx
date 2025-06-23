import React from 'react';

/**
 * Props for ScreenReaderOnly component
 */
export interface ScreenReaderOnlyProps {
  children: React.ReactNode;
  as?: React.ElementType;
  focusable?: boolean;
  className?: string;
  [key: string]: any; // Allow any additional props for the rendered element
}

/**
 * ScreenReaderOnly component
 *
 * Visually hides content but keeps it accessible to screen readers.
 * Can optionally be made focusable for skip links or similar functionality.
 *
 * @example
 * ```tsx
 * <ScreenReaderOnly>
 *   This text is only visible to screen readers
 * </ScreenReaderOnly>
 *
 * <ScreenReaderOnly focusable as="a" href="#main-content">
 *   Skip to main content
 * </ScreenReaderOnly>
 * ```
 */
export const ScreenReaderOnly: React.FC<ScreenReaderOnlyProps> = ({
  children,
  as: Component = 'span',
  focusable = false,
  className = '',
  ...props
}) => {
  const baseClasses = focusable ? 'sr-only' : 'sr-only';
  const combinedClasses = `${baseClasses} ${className}`.trim();

  return (
    <Component className={combinedClasses} {...props}>
      {children}
    </Component>
  );
};

/**
 * Hook to conditionally render content only for screen readers
 */
export function useScreenReaderOnly() {
  return {
    ScreenReaderOnly,
    /**
     * Utility function to create screen reader only text
     */
    createSRText: (text: string) => (
      <ScreenReaderOnly>{text}</ScreenReaderOnly>
    ),
    /**
     * Utility function to create focusable skip link
     */
    createSkipLink: (text: string, href: string) => (
      <ScreenReaderOnly as="a" href={href} focusable>
        {text}
      </ScreenReaderOnly>
    ),
  };
}

export default ScreenReaderOnly;