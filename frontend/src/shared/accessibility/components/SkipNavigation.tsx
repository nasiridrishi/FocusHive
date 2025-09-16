/**
 * Skip Navigation Component
 *
 * Provides skip links for keyboard users to navigate quickly
 * to main content areas, bypassing repetitive navigation.
 */

import React from 'react';
import {styled} from '@mui/material/styles';
import {Box, Link, List, ListItem} from '@mui/material';
import type {SkipLinkProps, SkipLinkTarget} from '../types/accessibility';

const SkipContainer = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'showOnFocus' && prop !== 'position'
})<{ showOnFocus?: boolean; position?: 'fixed' | 'absolute' }>(({
                                                                  theme,
                                                                  showOnFocus = true,
                                                                  position = 'fixed'
                                                                }) => ({
  position,
  top: 0,
  left: 0,
  zIndex: 9999,
  display: showOnFocus ? 'none' : 'block',

  '&:focus-within': showOnFocus ? {
    display: 'block'
  } : undefined,

  // High contrast support
  '@media (prefers-contrast: high)': {
    backgroundColor: theme.palette.common.black,
    color: theme.palette.common.white,
    border: '2px solid white',
  },

  // Reduced motion support
  '@media (prefers-reduced-motion: reduce)': {
    transition: 'none',
  }
}));

const SkipList = styled(List)(({theme}) => ({
  margin: 0,
  padding: 0,
  listStyle: 'none',
  backgroundColor: theme.palette.background.paper,
  boxShadow: theme.shadows[8],
  borderRadius: theme.shape.borderRadius,
  overflow: 'hidden',

  // Dark theme support
  ...(theme.palette.mode === 'dark' && {
    backgroundColor: theme.palette.grey[900],
    border: `1px solid ${theme.palette.grey[700]}`,
  })
}));

const SkipListItem = styled(ListItem)(({theme}) => ({
  padding: 0,

  '&:not(:last-child)': {
    borderBottom: `1px solid ${theme.palette.divider}`,
  }
}));

const SkipLink = styled(Link)(({theme}) => ({
  display: 'block',
  padding: theme.spacing(1.5, 2),
  color: theme.palette.text.primary,
  textDecoration: 'none',
  fontSize: '1rem',
  fontWeight: 500,
  lineHeight: 1.5,
  backgroundColor: 'transparent',
  border: 'none',
  width: '100%',
  textAlign: 'left',
  cursor: 'pointer',
  transition: theme.transitions.create(['background-color', 'color'], {
    duration: theme.transitions.duration.short,
  }),

  '&:hover, &:focus': {
    backgroundColor: theme.palette.primary.main,
    color: theme.palette.primary.contrastText,
    textDecoration: 'none',
  },

  '&:focus': {
    outline: `3px solid ${theme.palette.secondary.main}`,
    outlineOffset: '-3px',
  },

  // High contrast mode
  '@media (prefers-contrast: high)': {
    '&:focus': {
      outline: '3px solid currentColor',
    },
  },

  // Ensure sufficient touch target size
  minHeight: '44px',
  display: 'flex',
  alignItems: 'center',
}));

/**
 * Default skip targets for common page structures
 */
const DEFAULT_SKIP_TARGETS: SkipLinkTarget[] = [
  {
    id: 'main',
    label: 'Skip to main content',
  },
  {
    id: 'navigation',
    label: 'Skip to navigation',
  },
  {
    id: 'search',
    label: 'Skip to search',
  },
];

/**
 * Skip Navigation Component
 */
export const SkipNavigation: React.FC<SkipLinkProps> = ({
                                                          targets = DEFAULT_SKIP_TARGETS,
                                                          className,
                                                          showOnFocus = true,
                                                          position = 'fixed',
                                                          ...props
                                                        }) => {
  const handleSkipClick = (event: React.MouseEvent<HTMLAnchorElement>, target: SkipLinkTarget): HTMLElement | null => {
    event.preventDefault();

    // Find the target element
    const targetElement = document.getElementById(target.id);

    if (targetElement) {
      // Set focus to the target element
      targetElement.setAttribute('tabindex', '-1');
      targetElement.focus();

      // Remove tabindex after focus to avoid affecting normal tab order
      setTimeout(() => {
        targetElement.removeAttribute('tabindex');
      }, 0);

      // Scroll to target if needed (fallback for browsers that don't support focus scrolling)
      targetElement.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    } else if (target.href) {
      // Fallback to href navigation
      window.location.hash = target.href;
    } else {
      // console.warn(`Skip navigation target not found: ${target.id}`);
    }
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLAnchorElement>, target: SkipLinkTarget): void => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleSkipClick(event as unknown, target);
    }
  };

  if (targets.length === 0) {
    return null;
  }

  return (
      <SkipContainer
          className={className}
          showOnFocus={showOnFocus}
          position={position}
          role="navigation"
          aria-label="Skip navigation"
          {...props}
      >
        <SkipList>
          {targets.map((target, index) => (
              <SkipListItem key={target.id || index}>
                <SkipLink
                    href={target.href || `#${target.id}`}
                    onClick={(event) => handleSkipClick(event, target)}
                    onKeyDown={(event) => handleKeyDown(event, target)}
                    // First link should be focusable on page load
                    tabIndex={index === 0 ? 0 : -1}
                    // Screen reader context
                    aria-describedby={`skip-desc-${target.id}`}
                >
                  {target.label}
                </SkipLink>
                {/* Hidden description for screen readers */}
                <span
                    id={`skip-desc-${target.id}`}
                    style={{
                      position: 'absolute',
                      width: '1px',
                      height: '1px',
                      padding: 0,
                      margin: '-1px',
                      overflow: 'hidden',
                      clip: 'rect(0, 0, 0, 0)',
                      whiteSpace: 'nowrap',
                      border: 0,
                    }}
                >
              Press Enter to skip to {target.label.toLowerCase()}
            </span>
              </SkipListItem>
          ))}
        </SkipList>
      </SkipContainer>
  );
};

/**
 * Skip to Main Content Component (simplified version)
 */
export const SkipToMain: React.FC<{
  className?: string;
  showOnFocus?: boolean;
  mainId?: string;
}> = ({
        className,
        showOnFocus = true,
        mainId = 'main'
      }) => {
  const targets: SkipLinkTarget[] = [
    {
      id: mainId,
      label: 'Skip to main content',
    },
  ];

  return (
      <SkipNavigation
          targets={targets}
          className={className}
          showOnFocus={showOnFocus}
      />
  );
};

/**
 * Skip Navigation for complex layouts
 */
export const AdvancedSkipNavigation: React.FC<{
  className?: string;
  showOnFocus?: boolean;
  includeSearch?: boolean;
  includeSidebar?: boolean;
  includeFooter?: boolean;
  customTargets?: SkipLinkTarget[];
}> = ({
        className,
        showOnFocus = true,
        includeSearch = true,
        includeSidebar = true,
        includeFooter = false,
        customTargets = []
      }) => {
  const targets: SkipLinkTarget[] = [
    {
      id: 'main',
      label: 'Skip to main content',
    },
    {
      id: 'navigation',
      label: 'Skip to main navigation',
    },
    ...(includeSearch ? [{
      id: 'search',
      label: 'Skip to search',
    }] : []),
    ...(includeSidebar ? [{
      id: 'sidebar',
      label: 'Skip to sidebar',
    }] : []),
    ...(includeFooter ? [{
      id: 'footer',
      label: 'Skip to footer',
    }] : []),
    ...customTargets,
  ];

  return (
      <SkipNavigation
          targets={targets}
          className={className}
          showOnFocus={showOnFocus}
      />
  );
};

// Hook should be imported directly from '../hooks/useSkipNavigation' to avoid Fast Refresh warning

export default SkipNavigation;