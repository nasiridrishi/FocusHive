export interface SkipLink {
  id: string;
  label: string;
  target: string;
}

export const DEFAULT_SKIP_LINKS: SkipLink[] = [
  {
    id: 'skip-to-main',
    label: 'Skip to main content',
    target: '#main-content'
  },
  {
    id: 'skip-to-nav',
    label: 'Skip to navigation',
    target: '#main-navigation'
  },
  {
    id: 'skip-to-search',
    label: 'Skip to search',
    target: '#search'
  }
];

export function focusElement(selector: string): void {
  const element = document.querySelector(selector) as HTMLElement;
  if (element) {
    element.tabIndex = -1;
    element.focus();
    element.scrollIntoView({behavior: 'smooth'});
  }
}

export function announceToScreenReader(message: string): void {
  const announcement = document.createElement('div');
  announcement.setAttribute('role', 'status');
  announcement.setAttribute('aria-live', 'polite');
  announcement.className = 'sr-only';
  announcement.textContent = message;

  document.body.appendChild(announcement);

  setTimeout(() => {
    document.body.removeChild(announcement);
  }, 1000);
}