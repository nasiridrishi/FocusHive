/**
 * ARIA Utilities for Screen Reader Support
 *
 * Provides utilities for managing ARIA attributes, live regions,
 * and announcements for screen reader accessibility.
 */

/**
 * ARIA live region types
 */
export type AriaLive = 'off' | 'polite' | 'assertive';

/**
 * ARIA autocomplete values
 */
export type AriaAutoComplete = 'none' | 'inline' | 'list' | 'both';

/**
 * ARIA orientation values
 */
export type AriaOrientation = 'horizontal' | 'vertical';

/**
 * Common ARIA role definitions
 */
export const ARIA_ROLES = {
  // Landmark roles
  BANNER: 'banner',
  COMPLEMENTARY: 'complementary',
  CONTENTINFO: 'contentinfo',
  FORM: 'form',
  MAIN: 'main',
  NAVIGATION: 'navigation',
  REGION: 'region',
  SEARCH: 'search',

  // Widget roles
  ALERT: 'alert',
  ALERTDIALOG: 'alertdialog',
  BUTTON: 'button',
  CHECKBOX: 'checkbox',
  DIALOG: 'dialog',
  GRIDCELL: 'gridcell',
  LINK: 'link',
  LOG: 'log',
  MARQUEE: 'marquee',
  MENUITEM: 'menuitem',
  MENUITEMCHECKBOX: 'menuitemcheckbox',
  MENUITEMRADIO: 'menuitemradio',
  OPTION: 'option',
  PROGRESSBAR: 'progressbar',
  RADIO: 'radio',
  SCROLLBAR: 'scrollbar',
  SEARCHBOX: 'searchbox',
  SEPARATOR: 'separator',
  SLIDER: 'slider',
  SPINBUTTON: 'spinbutton',
  STATUS: 'status',
  SWITCH: 'switch',
  TAB: 'tab',
  TABPANEL: 'tabpanel',
  TEXTBOX: 'textbox',
  TIMER: 'timer',
  TOOLTIP: 'tooltip',
  TREEITEM: 'treeitem',

  // Composite roles
  COMBOBOX: 'combobox',
  GRID: 'grid',
  LISTBOX: 'listbox',
  MENU: 'menu',
  MENUBAR: 'menubar',
  RADIOGROUP: 'radiogroup',
  TABLIST: 'tablist',
  TREE: 'tree',
  TREEGRID: 'treegrid',

  // Document roles
  APPLICATION: 'application',
  ARTICLE: 'article',
  CELL: 'cell',
  COLUMNHEADER: 'columnheader',
  DEFINITION: 'definition',
  DIRECTORY: 'directory',
  DOCUMENT: 'document',
  FEED: 'feed',
  FIGURE: 'figure',
  GROUP: 'group',
  HEADING: 'heading',
  IMG: 'img',
  LIST: 'list',
  LISTITEM: 'listitem',
  MATH: 'math',
  NONE: 'none',
  NOTE: 'note',
  PRESENTATION: 'presentation',
  ROW: 'row',
  ROWGROUP: 'rowgroup',
  ROWHEADER: 'rowheader',
  TERM: 'term',
  TEXT: 'text',
  TOOLBAR: 'toolbar'
} as const;

/**
 * Common ARIA properties
 */
export interface AriaProperties {
  'aria-label'?: string;
  'aria-labelledby'?: string;
  'aria-describedby'?: string;
  'aria-details'?: string;
  'aria-expanded'?: boolean;
  'aria-hidden'?: boolean;
  'aria-live'?: AriaLive;
  'aria-atomic'?: boolean;
  'aria-relevant'?: string;
  'aria-busy'?: boolean;
  'aria-disabled'?: boolean;
  'aria-invalid'?: boolean | 'false' | 'true' | 'grammar' | 'spelling';
  'aria-required'?: boolean;
  'aria-readonly'?: boolean;
  'aria-selected'?: boolean;
  'aria-checked'?: boolean | 'mixed';
  'aria-pressed'?: boolean | 'mixed';
  'aria-current'?: boolean | 'page' | 'step' | 'location' | 'date' | 'time';
  'aria-orientation'?: AriaOrientation;
  'aria-autocomplete'?: AriaAutoComplete;
  'aria-haspopup'?: boolean | 'false' | 'true' | 'menu' | 'listbox' | 'tree' | 'grid' | 'dialog';
  'aria-controls'?: string;
  'aria-owns'?: string;
  'aria-flowto'?: string;
  'aria-activedescendant'?: string;
  'aria-level'?: number;
  'aria-multiline'?: boolean;
  'aria-multiselectable'?: boolean;
  'aria-placeholder'?: string;
  'aria-valuemax'?: number;
  'aria-valuemin'?: number;
  'aria-valuenow'?: number;
  'aria-valuetext'?: string;
  'aria-sort'?: 'none' | 'ascending' | 'descending' | 'other';
  'aria-grabbed'?: boolean;
  'aria-dropeffect'?: 'none' | 'copy' | 'execute' | 'link' | 'move' | 'popup';
}

/**
 * Generate unique IDs for ARIA attributes
 */
let idCounter = 0;

export function generateAriaId(prefix: string = 'aria'): string {
  return `${prefix}-${++idCounter}-${Date.now()}`;
}

/**
 * Create accessible name from multiple sources
 */
export function createAccessibleName(
    label?: string,
    labelledBy?: string,
    describedBy?: string,
    placeholder?: string
): AriaProperties {
  const props: AriaProperties = {};

  if (label) {
    props['aria-label'] = label;
  }

  if (labelledBy) {
    props['aria-labelledby'] = labelledBy;
  }

  if (describedBy) {
    props['aria-describedby'] = describedBy;
  }

  if (placeholder && !label && !labelledBy) {
    props['aria-placeholder'] = placeholder;
  }

  return props;
}

/**
 * Validate ARIA attributes
 */
interface AriaValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export function validateAriaAttributes(element: HTMLElement): AriaValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  const role = element.getAttribute('role');
  const ariaLabel = element.getAttribute('aria-label');
  const ariaLabelledBy = element.getAttribute('aria-labelledby');
  const ariaDescribedBy = element.getAttribute('aria-describedby');

  // Check for accessible name
  if (role && ['button', 'link', 'menuitem', 'option', 'tab'].includes(role)) {
    if (!ariaLabel && !ariaLabelledBy && !element.textContent?.trim()) {
      errors.push(`Element with role="${role}" must have an accessible name`);
    }
  }

  // Check for valid role
  if (role && !Object.values(ARIA_ROLES).includes(role as unknown)) {
    warnings.push(`Unknown ARIA role: "${role}"`);
  }

  // Check if labelledby references exist
  if (ariaLabelledBy) {
    const ids = ariaLabelledBy.split(' ');
    ids.forEach(id => {
      if (!document.getElementById(id)) {
        errors.push(`aria-labelledby references non-existent element: "${id}"`);
      }
    });
  }

  // Check if describedby references exist
  if (ariaDescribedBy) {
    const ids = ariaDescribedBy.split(' ');
    ids.forEach(id => {
      if (!document.getElementById(id)) {
        errors.push(`aria-describedby references non-existent element: "${id}"`);
      }
    });
  }

  return {
    valid: errors.length === 0,
    errors,
    warnings
  };
}

/**
 * Live region manager for announcements
 */
export class LiveRegionManager {
  private politeRegion: HTMLElement | null = null;
  private assertiveRegion: HTMLElement | null = null;
  private statusRegion: HTMLElement | null = null;

  constructor() {
    this.createRegions();
  }

  /**
   * Announce a message politely (non-interrupting)
   */
  announcePolite(message: string, delay: number = 0): void {
    if (!this.politeRegion) return;

    setTimeout(() => {
      if (this.politeRegion) {
        this.politeRegion.textContent = message;

        // Clear after a delay to allow re-announcements
        setTimeout(() => {
          if (this.politeRegion) {
            this.politeRegion.textContent = '';
          }
        }, 1000);
      }
    }, delay);
  }

  /**
   * Announce a message assertively (interrupting)
   */
  announceAssertive(message: string, delay: number = 0): void {
    if (!this.assertiveRegion) return;

    setTimeout(() => {
      if (this.assertiveRegion) {
        this.assertiveRegion.textContent = message;

        // Clear after a delay
        setTimeout(() => {
          if (this.assertiveRegion) {
            this.assertiveRegion.textContent = '';
          }
        }, 1000);
      }
    }, delay);
  }

  /**
   * Announce a status update
   */
  announceStatus(message: string, delay: number = 0): void {
    if (!this.statusRegion) return;

    setTimeout(() => {
      if (this.statusRegion) {
        this.statusRegion.textContent = message;

        // Clear after a delay
        setTimeout(() => {
          if (this.statusRegion) {
            this.statusRegion.textContent = '';
          }
        }, 1000);
      }
    }, delay);
  }

  /**
   * Clear all live regions
   */
  clear(): void {
    if (this.politeRegion) this.politeRegion.textContent = '';
    if (this.assertiveRegion) this.assertiveRegion.textContent = '';
    if (this.statusRegion) this.statusRegion.textContent = '';
  }

  /**
   * Cleanup regions
   */
  destroy(): void {
    if (this.politeRegion) {
      document.body.removeChild(this.politeRegion);
      this.politeRegion = null;
    }
    if (this.assertiveRegion) {
      document.body.removeChild(this.assertiveRegion);
      this.assertiveRegion = null;
    }
    if (this.statusRegion) {
      document.body.removeChild(this.statusRegion);
      this.statusRegion = null;
    }
  }

  private createRegions(): void {
    // Create polite live region
    this.politeRegion = document.createElement('div');
    this.politeRegion.setAttribute('aria-live', 'polite');
    this.politeRegion.setAttribute('aria-atomic', 'true');
    this.politeRegion.className = 'sr-only';
    this.politeRegion.style.cssText = `
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    `;

    // Create assertive live region
    this.assertiveRegion = document.createElement('div');
    this.assertiveRegion.setAttribute('aria-live', 'assertive');
    this.assertiveRegion.setAttribute('aria-atomic', 'true');
    this.assertiveRegion.className = 'sr-only';
    this.assertiveRegion.style.cssText = this.politeRegion.style.cssText;

    // Create status region
    this.statusRegion = document.createElement('div');
    this.statusRegion.setAttribute('role', 'status');
    this.statusRegion.setAttribute('aria-live', 'polite');
    this.statusRegion.setAttribute('aria-atomic', 'true');
    this.statusRegion.className = 'sr-only';
    this.statusRegion.style.cssText = this.politeRegion.style.cssText;

    // Add to DOM
    document.body.appendChild(this.politeRegion);
    document.body.appendChild(this.assertiveRegion);
    document.body.appendChild(this.statusRegion);
  }
}

/**
 * Global live region manager instance
 */
export const liveRegionManager = new LiveRegionManager();

/**
 * Screen reader utilities
 */
export const screenReaderUtils = {
  /**
   * Hide content from screen readers
   */
  hide: (element: HTMLElement) => {
    element.setAttribute('aria-hidden', 'true');
  },

  /**
   * Show content to screen readers
   */
  show: (element: HTMLElement) => {
    element.removeAttribute('aria-hidden');
  },

  /**
   * Create screen reader only text
   */
  createScreenReaderOnly: (text: string): HTMLElement => {
    const element = document.createElement('span');
    element.textContent = text;
    element.className = 'sr-only';
    element.style.cssText = `
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    `;
    return element;
  },

  /**
   * Announce to screen readers
   */
  announce: (message: string, priority: 'polite' | 'assertive' | 'status' = 'polite') => {
    switch (priority) {
      case 'assertive':
        liveRegionManager.announceAssertive(message);
        break;
      case 'status':
        liveRegionManager.announceStatus(message);
        break;
      default:
        liveRegionManager.announcePolite(message);
        break;
    }
  },

  /**
   * Get accessible name for an element
   */
  getAccessibleName: (element: HTMLElement): string => {
    // Check aria-label first
    const ariaLabel = element.getAttribute('aria-label');
    if (ariaLabel) return ariaLabel.trim();

    // Check aria-labelledby
    const ariaLabelledBy = element.getAttribute('aria-labelledby');
    if (ariaLabelledBy) {
      const labelElements = ariaLabelledBy.split(' ')
      .map(id => document.getElementById(id))
      .filter(el => el !== null)
      .map(el => el?.textContent || '')
      .join(' ');
      if (labelElements.trim()) return labelElements.trim();
    }

    // Check text content
    const textContent = element.textContent || '';
    if (textContent.trim()) return textContent.trim();

    // Check alt text for images
    if (element.tagName === 'IMG') {
      const alt = element.getAttribute('alt');
      if (alt) return alt.trim();
    }

    // Check title attribute
    const title = element.getAttribute('title');
    if (title) return title.trim();

    // Check placeholder for inputs
    if (element.tagName === 'INPUT' || element.tagName === 'TEXTAREA') {
      const placeholder = element.getAttribute('placeholder');
      if (placeholder) return placeholder.trim();
    }

    return '';
  },

  /**
   * Get accessible description for an element
   */
  getAccessibleDescription: (element: HTMLElement): string => {
    const ariaDescribedBy = element.getAttribute('aria-describedby');
    if (ariaDescribedBy) {
      const descriptionElements = ariaDescribedBy.split(' ')
      .map(id => document.getElementById(id))
      .filter(el => el !== null)
      .map(el => el?.textContent || '')
      .join(' ');
      if (descriptionElements.trim()) return descriptionElements.trim();
    }

    return '';
  }
};

/**
 * ARIA helper functions
 */
export const ariaHelpers = {
  /**
   * Create combobox ARIA attributes
   */
  combobox: (
      inputId: string,
      listboxId: string,
      expanded: boolean,
      activeDescendant?: string
  ): AriaProperties => ({
    role: ARIA_ROLES.COMBOBOX,
    'aria-expanded': expanded,
    'aria-controls': listboxId,
    'aria-autocomplete': 'list',
    ...(activeDescendant && {'aria-activedescendant': activeDescendant})
  }),

  /**
   * Create listbox ARIA attributes
   */
  listbox: (labelId?: string, multiselectable: boolean = false): AriaProperties => ({
    role: ARIA_ROLES.LISTBOX,
    ...(labelId && {'aria-labelledby': labelId}),
    ...(multiselectable && {'aria-multiselectable': true})
  }),

  /**
   * Create option ARIA attributes
   */
  option: (selected: boolean = false, disabled: boolean = false): AriaProperties => ({
    role: ARIA_ROLES.OPTION,
    'aria-selected': selected,
    ...(disabled && {'aria-disabled': true})
  }),

  /**
   * Create tab ARIA attributes
   */
  tab: (selected: boolean, controls: string, disabled: boolean = false): AriaProperties => ({
    role: ARIA_ROLES.TAB,
    'aria-selected': selected,
    'aria-controls': controls,
    ...(disabled && {'aria-disabled': true})
  }),

  /**
   * Create tabpanel ARIA attributes
   */
  tabpanel: (labelledBy: string): AriaProperties => ({
    role: ARIA_ROLES.TABPANEL,
    'aria-labelledby': labelledBy
  }),

  /**
   * Create dialog ARIA attributes
   */
  dialog: (labelId?: string, describedById?: string): AriaProperties => ({
    role: ARIA_ROLES.DIALOG,
    ...(labelId && {'aria-labelledby': labelId}),
    ...(describedById && {'aria-describedby': describedById})
  }),

  /**
   * Create alert ARIA attributes
   */
  alert: (live: AriaLive = 'assertive'): AriaProperties => ({
    role: ARIA_ROLES.ALERT,
    'aria-live': live,
    'aria-atomic': true
  }),

  /**
   * Create button toggle ARIA attributes
   */
  toggleButton: (pressed: boolean, disabled: boolean = false): AriaProperties => ({
    role: ARIA_ROLES.BUTTON,
    'aria-pressed': pressed,
    ...(disabled && {'aria-disabled': true})
  }),

  /**
   * Create disclosure ARIA attributes
   */
  disclosure: (expanded: boolean, controls: string): AriaProperties => ({
    'aria-expanded': expanded,
    'aria-controls': controls
  })
};