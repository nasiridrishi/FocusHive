/**
 * ARIA Testing Helpers
 *
 * Utility functions for testing ARIA implementation including:
 * - ARIA role validation
 * - Property and state verification
 * - Relationship testing
 * - Live region monitoring
 * - Complex pattern validation
 *
 * UOL-44.19: Comprehensive Accessibility E2E Tests
 */

import {Locator, Page} from '@playwright/test';

export interface AriaRole {
  name: string;
  requiredProps: string[];
  optionalProps: string[];
  requiredStates: string[];
  optionalStates: string[];
  requiredChildren?: string[];
  requiredParent?: string[];
  allowedChildren?: string[];
}

export interface AriaValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
  role?: string;
  properties: Record<string, string>;
  states: Record<string, string>;
}

export interface LiveRegionUpdate {
  content: string;
  timestamp: number;
  politeness: 'polite' | 'assertive' | 'off';
  atomic: boolean;
}

/**
 * ARIA roles and their specifications based on WAI-ARIA 1.1
 */
export const ARIA_ROLES: Record<string, AriaRole> = {
  alert: {
    name: 'alert',
    requiredProps: [],
    optionalProps: ['aria-atomic', 'aria-busy', 'aria-controls', 'aria-current', 'aria-describedby', 'aria-details', 'aria-flowto', 'aria-hidden', 'aria-keyshortcuts', 'aria-label', 'aria-labelledby', 'aria-live', 'aria-owns', 'aria-relevant', 'aria-roledescription'],
    requiredStates: [],
    optionalStates: []
  },

  alertdialog: {
    name: 'alertdialog',
    requiredProps: [],
    optionalProps: ['aria-modal', 'aria-labelledby', 'aria-describedby'],
    requiredStates: [],
    optionalStates: []
  },

  application: {
    name: 'application',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-label', 'aria-labelledby', 'aria-describedby'],
    requiredStates: [],
    optionalStates: []
  },

  button: {
    name: 'button',
    requiredProps: [],
    optionalProps: ['aria-expanded', 'aria-haspopup', 'aria-pressed', 'aria-label', 'aria-labelledby', 'aria-describedby'],
    requiredStates: [],
    optionalStates: ['aria-disabled']
  },

  checkbox: {
    name: 'checkbox',
    requiredProps: [],
    optionalProps: ['aria-label', 'aria-labelledby', 'aria-describedby', 'aria-readonly'],
    requiredStates: ['aria-checked'],
    optionalStates: ['aria-disabled', 'aria-invalid']
  },

  combobox: {
    name: 'combobox',
    requiredProps: ['aria-controls', 'aria-expanded'],
    optionalProps: ['aria-autocomplete', 'aria-haspopup', 'aria-label', 'aria-labelledby', 'aria-describedby', 'aria-activedescendant'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-invalid', 'aria-readonly', 'aria-required']
  },

  dialog: {
    name: 'dialog',
    requiredProps: [],
    optionalProps: ['aria-modal', 'aria-label', 'aria-labelledby', 'aria-describedby'],
    requiredStates: [],
    optionalStates: []
  },

  grid: {
    name: 'grid',
    requiredProps: [],
    optionalProps: ['aria-label', 'aria-labelledby', 'aria-describedby', 'aria-multiselectable', 'aria-readonly', 'aria-activedescendant'],
    requiredStates: [],
    optionalStates: [],
    allowedChildren: ['row', 'rowgroup']
  },

  gridcell: {
    name: 'gridcell',
    requiredProps: [],
    optionalProps: ['aria-colspan', 'aria-rowspan', 'aria-describedby', 'aria-label', 'aria-readonly'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-expanded', 'aria-selected'],
    requiredParent: ['row']
  },

  heading: {
    name: 'heading',
    requiredProps: ['aria-level'],
    optionalProps: ['aria-describedby', 'aria-label', 'aria-labelledby'],
    requiredStates: [],
    optionalStates: []
  },

  link: {
    name: 'link',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-label', 'aria-labelledby', 'aria-expanded', 'aria-haspopup'],
    requiredStates: [],
    optionalStates: ['aria-disabled']
  },

  list: {
    name: 'list',
    requiredProps: [],
    optionalProps: ['aria-label', 'aria-labelledby', 'aria-describedby'],
    requiredStates: [],
    optionalStates: [],
    allowedChildren: ['listitem', 'group']
  },

  listbox: {
    name: 'listbox',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-label', 'aria-labelledby', 'aria-describedby', 'aria-multiselectable', 'aria-required', 'aria-orientation'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-expanded', 'aria-invalid'],
    allowedChildren: ['option', 'group']
  },

  listitem: {
    name: 'listitem',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-label', 'aria-labelledby', 'aria-level', 'aria-posinset', 'aria-setsize'],
    requiredStates: [],
    optionalStates: [],
    requiredParent: ['list']
  },

  menu: {
    name: 'menu',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-label', 'aria-labelledby', 'aria-describedby', 'aria-orientation'],
    requiredStates: [],
    optionalStates: [],
    allowedChildren: ['menuitem', 'menuitemcheckbox', 'menuitemradio', 'group']
  },

  menubar: {
    name: 'menubar',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-label', 'aria-labelledby', 'aria-describedby', 'aria-orientation'],
    requiredStates: [],
    optionalStates: [],
    allowedChildren: ['menuitem', 'menuitemcheckbox', 'menuitemradio', 'group']
  },

  menuitem: {
    name: 'menuitem',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-haspopup', 'aria-label', 'aria-labelledby', 'aria-posinset', 'aria-setsize'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-expanded'],
    requiredParent: ['menu', 'menubar', 'group']
  },

  option: {
    name: 'option',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-label', 'aria-labelledby', 'aria-posinset', 'aria-setsize'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-selected'],
    requiredParent: ['listbox']
  },

  radio: {
    name: 'radio',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-label', 'aria-labelledby', 'aria-posinset', 'aria-setsize'],
    requiredStates: ['aria-checked'],
    optionalStates: ['aria-disabled']
  },

  row: {
    name: 'row',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-colspan', 'aria-describedby', 'aria-label', 'aria-labelledby', 'aria-level', 'aria-posinset', 'aria-rowindex', 'aria-setsize'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-expanded', 'aria-selected'],
    allowedChildren: ['cell', 'columnheader', 'gridcell', 'rowheader']
  },

  tab: {
    name: 'tab',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-haspopup', 'aria-label', 'aria-labelledby', 'aria-posinset', 'aria-setsize', 'aria-controls'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-expanded', 'aria-selected'],
    requiredParent: ['tablist']
  },

  tablist: {
    name: 'tablist',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-label', 'aria-labelledby', 'aria-describedby', 'aria-level', 'aria-multiselectable', 'aria-orientation'],
    requiredStates: [],
    optionalStates: [],
    allowedChildren: ['tab']
  },

  tabpanel: {
    name: 'tabpanel',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-label', 'aria-labelledby'],
    requiredStates: [],
    optionalStates: []
  },

  textbox: {
    name: 'textbox',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-autocomplete', 'aria-describedby', 'aria-haspopup', 'aria-label', 'aria-labelledby', 'aria-multiline', 'aria-placeholder'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-invalid', 'aria-readonly', 'aria-required']
  },

  tree: {
    name: 'tree',
    requiredProps: [],
    optionalProps: ['aria-activedescendant', 'aria-describedby', 'aria-label', 'aria-labelledby', 'aria-multiselectable', 'aria-orientation', 'aria-required'],
    requiredStates: [],
    optionalStates: [],
    allowedChildren: ['treeitem', 'group']
  },

  treeitem: {
    name: 'treeitem',
    requiredProps: [],
    optionalProps: ['aria-describedby', 'aria-haspopup', 'aria-label', 'aria-labelledby', 'aria-level', 'aria-posinset', 'aria-setsize'],
    requiredStates: [],
    optionalStates: ['aria-disabled', 'aria-expanded', 'aria-selected'],
    requiredParent: ['tree', 'group']
  }
};

export class AriaHelpers {
  constructor(private page: Page) {
  }

  /**
   * Validate ARIA implementation for an element
   */
  async validateAriaElement(element: Locator): Promise<AriaValidationResult> {
    const role = await element.getAttribute('role');
    const errors: string[] = [];
    const warnings: string[] = [];
    const properties: Record<string, string> = {};
    const states: Record<string, string> = {};

    // Get all ARIA attributes
    const ariaAttributes = await element.evaluate((el) => {
      const attrs: Record<string, string> = {};
      for (const attr of el.attributes) {
        if (attr.name.startsWith('aria-') || attr.name === 'role') {
          attrs[attr.name] = attr.value;
        }
      }
      return attrs;
    });

    // Separate properties and states
    for (const [name, value] of Object.entries(ariaAttributes)) {
      if (name === 'role') continue;

      if (['aria-checked', 'aria-disabled', 'aria-expanded', 'aria-hidden', 'aria-invalid', 'aria-pressed', 'aria-selected'].includes(name)) {
        states[name] = value;
      } else {
        properties[name] = value;
      }
    }

    if (!role) {
      // No role specified - validate native element semantics
      const tagName = await element.evaluate(el => el.tagName.toLowerCase());

      if (['div', 'span'].includes(tagName) && Object.keys(ariaAttributes).length > 1) {
        warnings.push(`Generic ${tagName} element with ARIA attributes should consider using semantic HTML or explicit role`);
      }
    } else {
      // Validate against role specification
      const roleSpec = ARIA_ROLES[role];

      if (!roleSpec) {
        errors.push(`Invalid or unsupported ARIA role: ${role}`);
      } else {
        // Check required properties
        for (const requiredProp of roleSpec.requiredProps) {
          if (!properties[requiredProp]) {
            errors.push(`Missing required property: ${requiredProp} for role ${role}`);
          }
        }

        // Check required states
        for (const requiredState of roleSpec.requiredStates) {
          if (!states[requiredState]) {
            errors.push(`Missing required state: ${requiredState} for role ${role}`);
          }
        }

        // Validate property values
        for (const [prop, value] of Object.entries(properties)) {
          const validationError = this.validateAriaPropertyValue(prop, value);
          if (validationError) {
            errors.push(validationError);
          }
        }

        // Validate state values
        for (const [state, value] of Object.entries(states)) {
          const validationError = this.validateAriaStateValue(state, value);
          if (validationError) {
            errors.push(validationError);
          }
        }

        // Check parent/child relationships
        if (roleSpec.requiredParent) {
          const hasValidParent = await this.checkParentRole(element, roleSpec.requiredParent);
          if (!hasValidParent) {
            errors.push(`Element with role ${role} must be contained within: ${roleSpec.requiredParent.join(', ')}`);
          }
        }

        if (roleSpec.allowedChildren) {
          const childrenValid = await this.checkChildrenRoles(element, roleSpec.allowedChildren);
          if (!childrenValid) {
            warnings.push(`Element with role ${role} should only contain: ${roleSpec.allowedChildren.join(', ')}`);
          }
        }
      }
    }

    // Check for referenced elements (aria-labelledby, aria-describedby, etc.)
    for (const [prop, value] of Object.entries(properties)) {
      if (['aria-labelledby', 'aria-describedby', 'aria-controls', 'aria-owns'].includes(prop)) {
        const referencedIds = value.split(' ');
        for (const id of referencedIds) {
          const referencedElement = this.page.locator(`#${id}`);
          if (await referencedElement.count() === 0) {
            errors.push(`Referenced element not found: ${prop}="${id}"`);
          }
        }
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
      role: role || undefined,
      properties,
      states
    };
  }

  /**
   * Monitor live region updates
   */
  async monitorLiveRegion(element: Locator, duration: number = 5000): Promise<LiveRegionUpdate[]> {
    const _updates: LiveRegionUpdate[] = [];

    // Set up mutation observer
    await element.evaluate((el, monitorDuration) => {
      const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.type === 'childList' || mutation.type === 'characterData') {
            const update = {
              content: el.textContent || '',
              timestamp: Date.now(),
              politeness: el.getAttribute('aria-live') || 'off',
              atomic: el.getAttribute('aria-atomic') === 'true'
            };

            // Store update in element for retrieval
            if (!(el as unknown as { __liveUpdates: LiveRegionUpdate[] }).__liveUpdates) {
              (el as unknown as { __liveUpdates: LiveRegionUpdate[] }).__liveUpdates = [];
            }
            (el as unknown as { __liveUpdates: LiveRegionUpdate[] }).__liveUpdates.push(update);
          }
        });
      });

      observer.observe(el, {
        childList: true,
        subtree: true,
        characterData: true
      });

      setTimeout(() => {
        observer.disconnect();
      }, monitorDuration);
    }, duration);

    // Wait for monitoring period
    await this.page.waitForTimeout(duration);

    // Retrieve updates
    const retrievedUpdates = await element.evaluate((el) => {
      return (el as unknown as { __liveUpdates?: LiveRegionUpdate[] }).__liveUpdates || [];
    });

    return retrievedUpdates;
  }

  /**
   * Test ARIA relationships
   */
  async testAriaRelationships(element: Locator): Promise<{
    labelledBy: boolean;
    describedBy: boolean;
    controls: boolean;
    owns: boolean;
  }> {
    const relationships = {
      labelledBy: true,
      describedBy: true,
      controls: true,
      owns: true
    };

    // Test aria-labelledby
    const labelledBy = await element.getAttribute('aria-labelledby');
    if (labelledBy) {
      const labelIds = labelledBy.split(' ');
      for (const id of labelIds) {
        const labelElement = this.page.locator(`#${id}`);
        if (await labelElement.count() === 0) {
          relationships.labelledBy = false;
          break;
        }
      }
    }

    // Test aria-describedby
    const describedBy = await element.getAttribute('aria-describedby');
    if (describedBy) {
      const descIds = describedBy.split(' ');
      for (const id of descIds) {
        const descElement = this.page.locator(`#${id}`);
        if (await descElement.count() === 0) {
          relationships.describedBy = false;
          break;
        }
      }
    }

    // Test aria-controls
    const controls = await element.getAttribute('aria-controls');
    if (controls) {
      const controlIds = controls.split(' ');
      for (const id of controlIds) {
        const controlElement = this.page.locator(`#${id}`);
        if (await controlElement.count() === 0) {
          relationships.controls = false;
          break;
        }
      }
    }

    // Test aria-owns
    const owns = await element.getAttribute('aria-owns');
    if (owns) {
      const ownIds = owns.split(' ');
      for (const id of ownIds) {
        const ownElement = this.page.locator(`#${id}`);
        if (await ownElement.count() === 0) {
          relationships.owns = false;
          break;
        }
      }
    }

    return relationships;
  }

  /**
   * Test focus management in ARIA widgets
   */
  async testFocusManagement(container: Locator): Promise<{
    hasActiveDescendant: boolean;
    activeDescendantValid: boolean;
    keyboardNavigable: boolean;
  }> {
    const activeDescendant = await container.getAttribute('aria-activedescendant');
    const hasActiveDescendant = !!activeDescendant;
    let activeDescendantValid = true;
    let keyboardNavigable = true;

    if (activeDescendant) {
      const activeElement = this.page.locator(`#${activeDescendant}`);
      activeDescendantValid = await activeElement.count() > 0;
    }

    // Test keyboard navigation
    await container.focus();

    const navigationKeys = ['ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];

    for (const key of navigationKeys.slice(0, 3)) {
      try {
        await this.page.keyboard.press(key);
        await this.page.waitForTimeout(100);

        // Check if active descendant changed or focus moved
        const currentActiveDescendant = await container.getAttribute('aria-activedescendant');
        const focusedElement = this.page.locator(':focus');

        if (currentActiveDescendant !== activeDescendant || await focusedElement.count() > 0) {
          // Navigation is working
          break;
        }
      } catch {
        keyboardNavigable = false;
      }
    }

    return {
      hasActiveDescendant,
      activeDescendantValid,
      keyboardNavigable
    };
  }

  /**
   * Validate complex ARIA patterns
   */
  async validateComplexPattern(element: Locator, pattern: string): Promise<AriaValidationResult> {
    const role = await element.getAttribute('role');
    const errors: string[] = [];
    const warnings: string[] = [];

    switch (pattern) {
      case 'tabs':
        if (role === 'tablist') {
          // Validate tab list pattern
          const tabs = await element.locator('[role="tab"]').all();
          const tabpanels = await this.page.locator('[role="tabpanel"]').all();

          if (tabs.length === 0) {
            errors.push('Tablist must contain tab elements');
          }

          if (tabpanels.length !== tabs.length) {
            warnings.push(`Tab count (${tabs.length}) does not match tabpanel count (${tabpanels.length})`);
          }

          // Check tab selection
          let selectedCount = 0;
          for (const tab of tabs) {
            const selected = await tab.getAttribute('aria-selected');
            if (selected === 'true') selectedCount++;
          }

          if (selectedCount !== 1) {
            errors.push(`Exactly one tab should be selected, found ${selectedCount}`);
          }
        }
        break;

      case 'combobox':
        if (role === 'combobox') {
          const expanded = await element.getAttribute('aria-expanded');
          const controls = await element.getAttribute('aria-controls');

          if (!expanded) {
            errors.push('Combobox must have aria-expanded attribute');
          }

          if (!controls) {
            errors.push('Combobox must have aria-controls attribute');
          } else {
            const listbox = this.page.locator(`#${controls}`);
            if (await listbox.count() === 0) {
              errors.push(`Combobox controls non-existent element: ${controls}`);
            }
          }
        }
        break;

      case 'menu':
        if (role === 'menu') {
          const menuItems = await element.locator('[role="menuitem"], [role="menuitemcheckbox"], [role="menuitemradio"]').all();

          if (menuItems.length === 0) {
            errors.push('Menu must contain menu items');
          }

          // Check for menu button
          const menuButton = this.page.locator(`[aria-haspopup][aria-controls="${await element.getAttribute('id')}"]`);
          if (await menuButton.count() === 0) {
            warnings.push('Menu should be associated with a menu button');
          }
        }
        break;

      case 'dialog':
        if (role === 'dialog' || role === 'alertdialog') {
          const modal = await element.getAttribute('aria-modal');

          if (modal !== 'true') {
            warnings.push('Dialog should have aria-modal="true"');
          }

          // Check for accessible name
          const label = await element.getAttribute('aria-label');
          const labelledBy = await element.getAttribute('aria-labelledby');

          if (!label && !labelledBy) {
            errors.push('Dialog must have accessible name (aria-label or aria-labelledby)');
          }
        }
        break;
    }

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
      role: role || undefined,
      properties: {},
      states: {}
    };
  }

  /**
   * Generate ARIA pattern suggestions
   */
  async generatePatternSuggestions(element: Locator): Promise<string[]> {
    const suggestions: string[] = [];
    const tagName = await element.evaluate(el => el.tagName.toLowerCase());
    const role = await element.getAttribute('role');
    const hasClickHandler = await element.evaluate(el => {
      return el.onclick !== null || el.addEventListener.toString().includes('click');
    });

    // Suggest improvements based on element analysis
    if (tagName === 'div' && hasClickHandler && !role) {
      suggestions.push('Consider using <button> element or role="button" for clickable div');
    }

    if (role === 'button') {
      const expanded = await element.getAttribute('aria-expanded');
      const controls = await element.getAttribute('aria-controls');

      if (expanded && !controls) {
        suggestions.push('Button with aria-expanded should have aria-controls');
      }
    }

    if (role === 'textbox') {
      const multiline = await element.getAttribute('aria-multiline');

      if (!multiline && tagName === 'textarea') {
        suggestions.push('Textarea should have aria-multiline="true"');
      }
    }

    return suggestions;
  }

  /**
   * Validate ARIA property value
   */
  private validateAriaPropertyValue(property: string, value: string): string | null {
    switch (property) {
      case 'aria-level': {
        const level = parseInt(value);
        if (isNaN(level) || level < 1) {
          return `aria-level must be a positive integer, got: ${value}`;
        }
        break;
      }

      case 'aria-posinset':
      case 'aria-setsize': {
        const num = parseInt(value);
        if (isNaN(num) || num < 1) {
          return `${property} must be a positive integer, got: ${value}`;
        }
        break;
      }

      case 'aria-colspan':
      case 'aria-rowspan': {
        const span = parseInt(value);
        if (isNaN(span) || span < 1) {
          return `${property} must be a positive integer, got: ${value}`;
        }
        break;
      }

      case 'aria-autocomplete':
        if (!['none', 'inline', 'list', 'both'].includes(value)) {
          return `aria-autocomplete must be one of: none, inline, list, both. Got: ${value}`;
        }
        break;

      case 'aria-orientation':
        if (!['horizontal', 'vertical', 'undefined'].includes(value)) {
          return `aria-orientation must be one of: horizontal, vertical, undefined. Got: ${value}`;
        }
        break;

      case 'aria-sort':
        if (!['ascending', 'descending', 'none', 'other'].includes(value)) {
          return `aria-sort must be one of: ascending, descending, none, other. Got: ${value}`;
        }
        break;

      case 'aria-live':
        if (!['off', 'polite', 'assertive'].includes(value)) {
          return `aria-live must be one of: off, polite, assertive. Got: ${value}`;
        }
        break;
    }

    return null;
  }

  /**
   * Validate ARIA state value
   */
  private validateAriaStateValue(state: string, value: string): string | null {
    switch (state) {
      case 'aria-checked':
        if (!['true', 'false', 'mixed'].includes(value)) {
          return `aria-checked must be true, false, or mixed. Got: ${value}`;
        }
        break;

      case 'aria-pressed':
        if (!['true', 'false', 'mixed'].includes(value)) {
          return `aria-pressed must be true, false, or mixed. Got: ${value}`;
        }
        break;

      case 'aria-selected':
      case 'aria-expanded':
      case 'aria-hidden':
      case 'aria-disabled':
      case 'aria-invalid':
      case 'aria-required':
      case 'aria-readonly':
      case 'aria-multiline':
      case 'aria-multiselectable':
      case 'aria-atomic':
        if (!['true', 'false'].includes(value)) {
          return `${state} must be true or false. Got: ${value}`;
        }
        break;
    }

    return null;
  }

  /**
   * Check if element has valid parent role
   */
  private async checkParentRole(element: Locator, requiredParents: string[]): Promise<boolean> {
    return element.evaluate((el, parents) => {
      let current = el.parentElement;
      while (current && current !== document.body) {
        const role = current.getAttribute('role');
        if (role && parents.includes(role)) {
          return true;
        }
        current = current.parentElement;
      }
      return false;
    }, requiredParents);
  }

  /**
   * Check if element's children have valid roles
   */
  private async checkChildrenRoles(element: Locator, allowedChildren: string[]): Promise<boolean> {
    return element.evaluate((el, allowed) => {
      const children = Array.from(el.children);
      return children.every(child => {
        const role = child.getAttribute('role');
        return !role || allowed.includes(role);
      });
    }, allowedChildren);
  }
}