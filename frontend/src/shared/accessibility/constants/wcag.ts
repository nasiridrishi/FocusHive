/**
 * WCAG 2.1 Constants and Standards
 * 
 * Defines constants and guidelines for WCAG 2.1 AA/AAA compliance
 */

/**
 * WCAG 2.1 Success Criteria Levels
 */
export const WCAG_LEVELS = {
  A: 'A',
  AA: 'AA',
  AAA: 'AAA'
} as const;

export type WCAGLevel = typeof WCAG_LEVELS[keyof typeof WCAG_LEVELS];

/**
 * Contrast Ratio Requirements
 */
export const CONTRAST_RATIOS = {
  // WCAG AA Requirements
  AA_NORMAL_TEXT: 4.5,      // 18pt or smaller, or 14pt bold or smaller
  AA_LARGE_TEXT: 3.0,       // 18pt or larger, or 14pt bold or larger
  AA_GRAPHICS: 3.0,         // Non-text elements like icons, charts
  AA_UI_COMPONENTS: 3.0,    // Interactive components and their states
  
  // WCAG AAA Requirements
  AAA_NORMAL_TEXT: 7.0,
  AAA_LARGE_TEXT: 4.5,
  AAA_GRAPHICS: 4.5,
  AAA_UI_COMPONENTS: 4.5,
  
  // Focus indicators
  FOCUS_INDICATOR: 3.0,     // Minimum contrast for focus indicators
  
  // Minimum ratios for different purposes
  MINIMUM_READABLE: 2.0,    // Barely readable (not WCAG compliant)
  PREFERRED_MINIMUM: 5.0    // Recommended minimum for good readability
} as const;

/**
 * Text Size Classifications for Contrast
 */
export const TEXT_SIZES = {
  // Normal text (requires higher contrast)
  NORMAL: {
    maxFontSize: 18,
    maxBoldFontSize: 14,
    contrastAA: CONTRAST_RATIOS.AA_NORMAL_TEXT,
    contrastAAA: CONTRAST_RATIOS.AAA_NORMAL_TEXT
  },
  
  // Large text (allows lower contrast)
  LARGE: {
    minFontSize: 18,
    minBoldFontSize: 14,
    contrastAA: CONTRAST_RATIOS.AA_LARGE_TEXT,
    contrastAAA: CONTRAST_RATIOS.AAA_LARGE_TEXT
  }
} as const;

/**
 * Touch Target Sizes (WCAG 2.1 AAA)
 */
export const TOUCH_TARGETS = {
  // Minimum touch target size
  MIN_SIZE: 44,             // 44x44 pixels minimum
  MIN_SIZE_CSS: '44px',
  
  // Recommended touch target size
  RECOMMENDED_SIZE: 48,     // 48x48 pixels recommended
  RECOMMENDED_SIZE_CSS: '48px',
  
  // Minimum spacing between touch targets
  MIN_SPACING: 8,           // 8px minimum spacing
  MIN_SPACING_CSS: '8px',
  
  // Recommended spacing
  RECOMMENDED_SPACING: 16,  // 16px recommended spacing
  RECOMMENDED_SPACING_CSS: '16px'
} as const;

/**
 * Animation and Motion Preferences
 */
export const MOTION_PREFERENCES = {
  // Reduced motion settings
  REDUCED_MOTION_QUERY: '(prefers-reduced-motion: reduce)',
  NO_PREFERENCE_QUERY: '(prefers-reduced-motion: no-preference)',
  
  // Safe animation durations for reduced motion
  REDUCED_DURATION: 0.01,   // Nearly instant
  NORMAL_DURATION: 0.3,     // Normal animation duration
  
  // Animation types that should respect reduced motion
  ANIMATIONS_TO_REDUCE: [
    'transform',
    'opacity',
    'filter',
    'background-position'
  ]
} as const;

/**
 * Color Vision Deficiency Types
 */
export const COLOR_VISION_TYPES = {
  PROTANOMALY: 'protanomaly',       // Reduced red sensitivity
  PROTANOPIA: 'protanopia',         // No red sensitivity
  DEUTERANOMALY: 'deuteranomaly',   // Reduced green sensitivity
  DEUTERANOPIA: 'deuteranopia',     // No green sensitivity
  TRITANOMALY: 'tritanomaly',       // Reduced blue sensitivity
  TRITANOPIA: 'tritanopia',         // No blue sensitivity
  ACHROMATOPSIA: 'achromatopsia',   // Complete color blindness
  ACHROMATOMALY: 'achromatomaly'    // Partial color blindness
} as const;

/**
 * Keyboard Navigation Constants
 */
export const KEYBOARD = {
  // Navigation keys
  ARROW_UP: 'ArrowUp',
  ARROW_DOWN: 'ArrowDown',
  ARROW_LEFT: 'ArrowLeft',
  ARROW_RIGHT: 'ArrowRight',
  HOME: 'Home',
  END: 'End',
  PAGE_UP: 'PageUp',
  PAGE_DOWN: 'PageDown',
  
  // Action keys
  ENTER: 'Enter',
  SPACE: ' ',
  TAB: 'Tab',
  ESCAPE: 'Escape',
  DELETE: 'Delete',
  BACKSPACE: 'Backspace',
  
  // Modifier keys
  SHIFT: 'Shift',
  CTRL: 'Control',
  ALT: 'Alt',
  META: 'Meta',
  
  // Common key combinations
  CTRL_A: 'Control+a',
  CTRL_C: 'Control+c',
  CTRL_V: 'Control+v',
  CTRL_Z: 'Control+z'
} as const;

/**
 * Screen Reader Delay Constants
 */
export const SCREEN_READER_DELAYS = {
  // Delays for different announcement types
  IMMEDIATE: 0,
  SHORT: 100,
  MEDIUM: 300,
  LONG: 500,
  
  // Delays for specific scenarios
  AFTER_FOCUS_CHANGE: 100,
  AFTER_PAGE_LOAD: 300,
  AFTER_ROUTE_CHANGE: 500,
  AFTER_MODAL_OPEN: 200,
  
  // Cleanup delays
  CLEAR_ANNOUNCEMENT: 1000,
  CLEAR_STATUS: 2000
} as const;

/**
 * Focus Management Constants
 */
export const FOCUS = {
  // Focus ring styles
  FOCUS_RING_WIDTH: 2,
  FOCUS_RING_OFFSET: 2,
  FOCUS_RING_STYLE: 'solid',
  
  // Focus timing
  FOCUS_DELAY: 0,           // No delay for immediate focus
  RESTORE_FOCUS_DELAY: 100, // Small delay when restoring focus
  
  // Focus trap settings
  TRAP_FOCUS_SELECTOR: '[data-focus-trap]',
  TRAP_INITIAL_FOCUS: '[data-focus-initial]',
  TRAP_RETURN_FOCUS: '[data-focus-return]'
} as const;

/**
 * Form Validation Constants
 */
export const FORM_VALIDATION = {
  // Error message timing
  SHOW_ERROR_DELAY: 0,      // Show errors immediately
  CLEAR_ERROR_DELAY: 5000,  // Clear errors after 5 seconds
  
  // Validation triggers
  VALIDATE_ON_BLUR: true,
  VALIDATE_ON_CHANGE: false,
  VALIDATE_ON_SUBMIT: true,
  
  // Error message attributes
  ERROR_ROLE: 'alert',
  ERROR_LIVE: 'assertive',
  ERROR_ATOMIC: true
} as const;

/**
 * Modal and Dialog Constants
 */
export const MODAL = {
  // Default z-index values
  BACKDROP_Z_INDEX: 1300,
  MODAL_Z_INDEX: 1310,
  
  // Animation durations
  ENTER_DURATION: 225,
  EXIT_DURATION: 195,
  
  // Focus management
  FOCUS_INITIAL_SELECTOR: '[autofocus], [data-autofocus]',
  FOCUS_RETURN_DELAY: 100,
  
  // Backdrop behavior
  CLOSE_ON_BACKDROP_CLICK: true,
  CLOSE_ON_ESCAPE: true
} as const;

/**
 * Loading and Status Constants
 */
export const LOADING = {
  // Minimum loading time to avoid flicker
  MIN_LOADING_TIME: 200,
  
  // Skeleton loading
  SKELETON_ANIMATION_DURATION: 1.5,
  
  // Status announcements
  LOADING_ANNOUNCEMENT_DELAY: 1000,  // Announce loading after 1 second
  LOADED_ANNOUNCEMENT: 'Content loaded',
  ERROR_ANNOUNCEMENT: 'Error loading content'
} as const;

/**
 * Responsive Design Constants
 */
export const RESPONSIVE = {
  // Breakpoint values (in pixels)
  MOBILE_MAX: 767,
  TABLET_MIN: 768,
  TABLET_MAX: 1023,
  DESKTOP_MIN: 1024,
  
  // Touch vs mouse detection
  FINE_POINTER_QUERY: '(pointer: fine)',
  COARSE_POINTER_QUERY: '(pointer: coarse)',
  
  // Hover capability
  CAN_HOVER_QUERY: '(hover: hover)',
  NO_HOVER_QUERY: '(hover: none)'
} as const;

/**
 * Notification and Alert Constants
 */
export const NOTIFICATIONS = {
  // Auto-dismiss timing
  SUCCESS_DURATION: 4000,
  INFO_DURATION: 6000,
  WARNING_DURATION: 8000,
  ERROR_DURATION: 0,        // Errors require manual dismissal
  
  // Positioning
  DEFAULT_POSITION: 'top-right',
  MOBILE_POSITION: 'top-center',
  
  // Announcement priorities
  SUCCESS_PRIORITY: 'polite',
  INFO_PRIORITY: 'polite',
  WARNING_PRIORITY: 'assertive',
  ERROR_PRIORITY: 'assertive'
} as const;

/**
 * WCAG Success Criteria Mapping
 */
export const SUCCESS_CRITERIA = {
  // Level A
  '1.1.1': { level: 'A', name: 'Non-text Content' },
  '1.2.1': { level: 'A', name: 'Audio-only and Video-only (Prerecorded)' },
  '1.2.2': { level: 'A', name: 'Captions (Prerecorded)' },
  '1.2.3': { level: 'A', name: 'Audio Description or Media Alternative (Prerecorded)' },
  '1.3.1': { level: 'A', name: 'Info and Relationships' },
  '1.3.2': { level: 'A', name: 'Meaningful Sequence' },
  '1.3.3': { level: 'A', name: 'Sensory Characteristics' },
  '1.4.1': { level: 'A', name: 'Use of Color' },
  '1.4.2': { level: 'A', name: 'Audio Control' },
  
  // Level AA
  '1.2.4': { level: 'AA', name: 'Captions (Live)' },
  '1.2.5': { level: 'AA', name: 'Audio Description (Prerecorded)' },
  '1.3.4': { level: 'AA', name: 'Orientation' },
  '1.3.5': { level: 'AA', name: 'Identify Input Purpose' },
  '1.4.3': { level: 'AA', name: 'Contrast (Minimum)' },
  '1.4.4': { level: 'AA', name: 'Resize text' },
  '1.4.5': { level: 'AA', name: 'Images of Text' },
  '1.4.10': { level: 'AA', name: 'Reflow' },
  '1.4.11': { level: 'AA', name: 'Non-text Contrast' },
  '1.4.12': { level: 'AA', name: 'Text Spacing' },
  '1.4.13': { level: 'AA', name: 'Content on Hover or Focus' },
  
  // Level AAA
  '1.2.6': { level: 'AAA', name: 'Sign Language (Prerecorded)' },
  '1.2.7': { level: 'AAA', name: 'Extended Audio Description (Prerecorded)' },
  '1.2.8': { level: 'AAA', name: 'Media Alternative (Prerecorded)' },
  '1.2.9': { level: 'AAA', name: 'Audio-only (Live)' },
  '1.4.6': { level: 'AAA', name: 'Contrast (Enhanced)' },
  '1.4.7': { level: 'AAA', name: 'Low or No Background Audio' },
  '1.4.8': { level: 'AAA', name: 'Visual Presentation' },
  '1.4.9': { level: 'AAA', name: 'Images of Text (No Exception)' }
} as const;