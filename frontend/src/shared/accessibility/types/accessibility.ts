/**
 * TypeScript types for accessibility features
 */

import { AriaProperties } from '../utils/ariaUtils';
import { WCAGLevel } from '../constants/wcag';

/**
 * Focus management types
 */
export interface FocusableElement extends HTMLElement {
  focus(): void;
  blur(): void;
}

export interface FocusTrapConfig {
  initialFocus?: HTMLElement | string | null;
  restoreFocus?: HTMLElement | string | null;
  onEscape?: () => void;
  allowOutsideClick?: boolean;
  preventScroll?: boolean;
}

export interface KeyboardNavigationConfig {
  orientation?: 'horizontal' | 'vertical' | 'both';
  wrap?: boolean;
  loop?: boolean;
  roving?: boolean;
  onNavigate?: (element: HTMLElement, direction: string) => void;
  onSelect?: (element: HTMLElement) => void;
}

/**
 * Screen reader and announcement types
 */
export type AnnounceLevel = 'polite' | 'assertive' | 'status';

export interface AnnouncementOptions {
  level?: AnnounceLevel;
  delay?: number;
  clearAfter?: number;
  interrupt?: boolean;
}

export interface LiveRegionProps {
  'aria-live'?: 'off' | 'polite' | 'assertive';
  'aria-atomic'?: boolean;
  'aria-relevant'?: 'additions' | 'removals' | 'text' | 'all';
  'aria-label'?: string;
}

/**
 * Color and contrast types
 */
export interface ContrastRatio {
  ratio: number;
  level: WCAGLevel;
  passes: boolean;
  textSize?: 'normal' | 'large';
}

export interface ColorValidation {
  foreground: string;
  background: string;
  contrast: ContrastRatio;
  recommendation?: string;
}

export interface AccessibilityTheme {
  focusRing: {
    color: string;
    width: number;
    offset: number;
    style: 'solid' | 'dashed' | 'dotted';
  };
  highContrast: {
    text: string;
    background: string;
    border: string;
    accent: string;
  };
  reducedMotion: {
    duration: number;
    easing: string;
  };
}

/**
 * Keyboard interaction types
 */
export interface KeyboardEvent extends Event {
  key: string;
  code: string;
  ctrlKey: boolean;
  shiftKey: boolean;
  altKey: boolean;
  metaKey: boolean;
}

export interface KeyboardHandler {
  key: string | string[];
  handler: (event: KeyboardEvent) => void | boolean;
  modifier?: 'ctrl' | 'shift' | 'alt' | 'meta';
  preventDefault?: boolean;
  stopPropagation?: boolean;
}

export type NavigationDirection = 'up' | 'down' | 'left' | 'right' | 'first' | 'last';

/**
 * Form accessibility types
 */
export interface FieldValidation {
  isValid: boolean;
  errorMessage?: string;
  warningMessage?: string;
  successMessage?: string;
}

export interface FormFieldProps extends AriaProperties {
  id: string;
  name: string;
  label?: string;
  description?: string;
  error?: string;
  warning?: string;
  required?: boolean;
  disabled?: boolean;
  readOnly?: boolean;
  placeholder?: string;
  autoComplete?: string;
}

export interface AccessibilityFormState {
  errors: Record<string, string>;
  warnings: Record<string, string>;
  touched: Record<string, boolean>;
  submitted: boolean;
}

/**
 * Component accessibility props
 */
export interface AccessibilityProps extends AriaProperties {
  // Focus management
  autoFocus?: boolean;
  tabIndex?: number;
  onFocus?: (event: React.FocusEvent) => void;
  onBlur?: (event: React.FocusEvent) => void;
  
  // Keyboard interaction
  onKeyDown?: (event: React.KeyboardEvent) => void;
  onKeyUp?: (event: React.KeyboardEvent) => void;
  
  // Screen reader
  screenReaderLabel?: string;
  announceOnChange?: boolean;
  announcementLevel?: AnnounceLevel;
  
  // Visual accessibility
  highContrast?: boolean;
  reducedMotion?: boolean;
  forceFocusVisible?: boolean;
}

/**
 * Modal and dialog types
 */
export interface ModalAccessibilityProps {
  isOpen: boolean;
  onClose: () => void;
  initialFocusRef?: React.RefObject<HTMLElement>;
  restoreFocusRef?: React.RefObject<HTMLElement>;
  closeOnEscape?: boolean;
  closeOnBackdropClick?: boolean;
  trapFocus?: boolean;
  preventScroll?: boolean;
  ariaLabel?: string;
  ariaLabelledBy?: string;
  ariaDescribedBy?: string;
}

/**
 * Skip link types
 */
export interface SkipLinkTarget {
  id: string;
  label: string;
  href?: string;
}

export interface SkipLinkProps {
  targets: SkipLinkTarget[];
  className?: string;
  showOnFocus?: boolean;
  position?: 'fixed' | 'absolute';
}

/**
 * Accessible table types
 */
export interface TableAccessibilityProps {
  caption?: string;
  summary?: string;
  sortable?: boolean;
  currentSort?: {
    column: string;
    direction: 'asc' | 'desc';
  };
  onSort?: (column: string, direction: 'asc' | 'desc') => void;
  ariaLabel?: string;
  ariaLabelledBy?: string;
}

export interface TableCellProps {
  isHeader?: boolean;
  scope?: 'col' | 'row' | 'colgroup' | 'rowgroup';
  abbr?: string;
  sortDirection?: 'asc' | 'desc' | 'none';
}

/**
 * Media accessibility types
 */
export interface MediaAccessibilityProps {
  hasAudio?: boolean;
  hasVideo?: boolean;
  hasSubtitles?: boolean;
  hasAudioDescription?: boolean;
  hasTranscript?: boolean;
  autoPlay?: boolean;
  controls?: boolean;
  muted?: boolean;
}

/**
 * Touch and pointer types
 */
export interface TouchTargetProps {
  minSize?: number;
  minSpacing?: number;
  shape?: 'circle' | 'square' | 'rounded';
}

export interface PointerSupport {
  fine: boolean;
  coarse: boolean;
  hover: boolean;
}

/**
 * Landmark and region types
 */
export type LandmarkRole = 
  | 'banner'
  | 'complementary'
  | 'contentinfo'
  | 'form'
  | 'main'
  | 'navigation'
  | 'region'
  | 'search';

export interface LandmarkProps {
  role?: LandmarkRole;
  ariaLabel?: string;
  ariaLabelledBy?: string;
}

/**
 * Accessibility testing types
 */
export interface AccessibilityTest {
  name: string;
  description: string;
  selector?: string;
  check: (element: HTMLElement) => AccessibilityTestResult;
}

export interface AccessibilityTestResult {
  passed: boolean;
  message: string;
  severity: 'error' | 'warning' | 'info';
  helpUrl?: string;
  element?: HTMLElement;
}

export interface AccessibilityAudit {
  url?: string;
  timestamp: Date;
  results: AccessibilityTestResult[];
  summary: {
    errors: number;
    warnings: number;
    passed: number;
    total: number;
  };
}

/**
 * User preference types
 */
export interface UserPreferences {
  reducedMotion: boolean;
  highContrast: boolean;
  largeFonts: boolean;
  screenReader: boolean;
  keyboardNavigation: boolean;
  colorBlindness?: string;
}

export interface AccessibilitySettings {
  theme: 'light' | 'dark' | 'high-contrast';
  fontSize: 'small' | 'medium' | 'large' | 'extra-large';
  motion: 'full' | 'reduced' | 'none';
  focus: 'default' | 'enhanced';
  announcements: 'all' | 'important' | 'none';
}

/**
 * Hook return types
 */
export interface UseFocusTrapReturn {
  activate: () => void;
  deactivate: () => void;
  isActive: boolean;
  focusableElements: HTMLElement[];
}

export interface UseAnnouncementReturn {
  announce: (message: string, options?: AnnouncementOptions) => void;
  clear: () => void;
  lastAnnouncement: string | null;
}

export interface UseKeyboardNavigationReturn {
  currentIndex: number;
  setCurrentIndex: (index: number) => void;
  focusNext: () => void;
  focusPrevious: () => void;
  focusFirst: () => void;
  focusLast: () => void;
  handleKeyDown: (event: React.KeyboardEvent) => void;
}

export interface UseAccessibilityReturn {
  // User preferences
  preferences: UserPreferences;
  updatePreferences: (preferences: Partial<UserPreferences>) => void;
  
  // Announcement system
  announce: (message: string, level?: AnnounceLevel) => void;
  
  // Focus management
  focusTrap: UseFocusTrapReturn;
  
  // Color and contrast
  validateContrast: (fg: string, bg: string) => ContrastRatio;
  
  // Testing
  runAudit: (container?: HTMLElement) => Promise<AccessibilityAudit>;
}

/**
 * Event types for accessibility
 */
export interface AccessibilityEventMap {
  'accessibility:announce': CustomEvent<{ message: string; level: AnnounceLevel }>;
  'accessibility:focus-change': CustomEvent<{ previous: HTMLElement | null; current: HTMLElement | null }>;
  'accessibility:preference-change': CustomEvent<{ preference: keyof UserPreferences; value: unknown }>;
  'accessibility:violation': CustomEvent<{ test: string; element: HTMLElement; severity: 'error' | 'warning' }>;
}