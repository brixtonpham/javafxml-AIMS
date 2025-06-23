import React from 'react';

/**
 * Props for SkipLink component
 */
export interface SkipLinkProps {
  href: string;
  children: React.ReactNode;
  className?: string;
}

/**
 * SkipLink component
 * 
 * Provides a keyboard-accessible way to skip to main content or other sections.
 * The link is visually hidden until focused, making it available for keyboard users.
 * 
 * @example
 * ```tsx
 * <SkipLink href="#main-content">
 *   Skip to main content
 * </SkipLink>
 * 
 * <SkipLink href="#navigation">
 *   Skip to navigation
 * </SkipLink>
 * ```
 */
export const SkipLink: React.FC<SkipLinkProps> = ({
  href,
  children,
  className = '',
}) => {
  const handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    
    // Find the target element
    const targetId = href.replace('#', '');
    const targetElement = document.getElementById(targetId);
    
    if (targetElement) {
      // Focus the target element
      targetElement.focus();
      
      // If the element is not naturally focusable, add tabindex temporarily
      if (!targetElement.getAttribute('tabindex')) {
        targetElement.setAttribute('tabindex', '-1');
        // Remove tabindex after focus to maintain natural tab order
        targetElement.addEventListener('blur', () => {
          targetElement.removeAttribute('tabindex');
        }, { once: true });
      }
      
      // Scroll to the element smoothly
      targetElement.scrollIntoView({ 
        behavior: 'smooth', 
        block: 'start' 
      });
    }
  };

  return (
    <a
      href={href}
      onClick={handleClick}
      className={`skip-link ${className}`.trim()}
    >
      {children}
    </a>
  );
};

/**
 * SkipLinks component - Container for multiple skip links
 */
export interface SkipLinksProps {
  children: React.ReactNode;
  className?: string;
}

export const SkipLinks: React.FC<SkipLinksProps> = ({
  children,
  className = '',
}) => {
  return (
    <nav 
      className={`skip-links-container ${className}`.trim()}
      aria-label="Skip navigation"
    >
      {children}
    </nav>
  );
};

/**
 * Common skip links for typical page structures
 */
export const CommonSkipLinks: React.FC = () => {
  return (
    <SkipLinks>
      <SkipLink href="#main-content">
        Skip to main content
      </SkipLink>
      <SkipLink href="#navigation">
        Skip to navigation
      </SkipLink>
      <SkipLink href="#search">
        Skip to search
      </SkipLink>
      <SkipLink href="#footer">
        Skip to footer
      </SkipLink>
    </SkipLinks>
  );
};

export default SkipLink;