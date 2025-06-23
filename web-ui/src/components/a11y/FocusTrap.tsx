import React, { useEffect, useRef, useCallback } from 'react';
import { useFocusManagement } from '../../hooks/useAccessibility';

/**
 * Props for FocusTrap component
 */
export interface FocusTrapProps {
  children: React.ReactNode;
  enabled?: boolean;
  restoreFocus?: boolean;
  className?: string;
  onEscape?: () => void;
}

/**
 * FocusTrap component
 * 
 * Traps focus within its children, useful for modals, dropdowns, and other overlays.
 * When enabled, focus will cycle through focusable elements within the trap.
 * 
 * @example
 * ```tsx
 * <FocusTrap enabled={isModalOpen} onEscape={() => setIsModalOpen(false)}>
 *   <div className="modal">
 *     <h2>Modal Title</h2>
 *     <button>Action</button>
 *     <button onClick={() => setIsModalOpen(false)}>Close</button>
 *   </div>
 * </FocusTrap>
 * ```
 */
export const FocusTrap: React.FC<FocusTrapProps> = ({
  children,
  enabled = true,
  restoreFocus = true,
  className = '',
  onEscape,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const { trapFocus, restoreFocus: restorePreviousFocus } = useFocusManagement();
  const cleanupRef = useRef<(() => void) | null>(null);

  // Handle escape key
  const handleKeyDown = useCallback((event: KeyboardEvent) => {
    if (event.key === 'Escape' && onEscape) {
      event.preventDefault();
      event.stopPropagation();
      onEscape();
    }
  }, [onEscape]);

  useEffect(() => {
    if (!enabled || !containerRef.current) return;

    const container = containerRef.current;
    
    // Set up focus trap
    cleanupRef.current = trapFocus(container);

    // Add escape key listener
    if (onEscape) {
      document.addEventListener('keydown', handleKeyDown, true);
    }

    // Cleanup function
    return () => {
      if (cleanupRef.current) {
        cleanupRef.current();
        cleanupRef.current = null;
      }

      if (onEscape) {
        document.removeEventListener('keydown', handleKeyDown, true);
      }

      // Restore focus when trap is disabled
      if (restoreFocus) {
        restorePreviousFocus();
      }
    };
  }, [enabled, handleKeyDown, onEscape, restoreFocus, restorePreviousFocus, trapFocus]);

  return (
    <div 
      ref={containerRef}
      className={className}
      // Ensure the container is focusable for the trap to work
      tabIndex={-1}
    >
      {children}
    </div>
  );
};

export default FocusTrap;