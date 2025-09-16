import React from 'react';

export interface SkipLinkTarget {
  id: string;
  label: string;
  selector: string;
  order?: number;
}

const DEFAULT_SKIP_TARGETS: SkipLinkTarget[] = [
  {id: 'main', label: 'Skip to main content', selector: '#main-content', order: 1},
  {id: 'nav', label: 'Skip to navigation', selector: '#main-nav', order: 2},
  {id: 'search', label: 'Skip to search', selector: '#search', order: 3},
];

/**
 * Hook for managing skip navigation targets
 */
export function useSkipNavigation(): unknown[] {
  const [targets, setTargets] = React.useState<SkipLinkTarget[]>(DEFAULT_SKIP_TARGETS);

  const addTarget = React.useCallback((target: SkipLinkTarget) => {
    setTargets(prev => {
      // Avoid duplicates
      if (prev.some(t => t.id === target.id)) {
        return prev;
      }
      return [...prev, target];
    });
  }, []);

  const removeTarget = React.useCallback((targetId: string) => {
    setTargets(prev => prev.filter(target => target.id !== targetId));
  }, []);

  const updateTarget = React.useCallback((targetId: string, updates: Partial<SkipLinkTarget>) => {
    setTargets(prev => prev.map(target =>
        target.id === targetId ? {...target, ...updates} : target
    ));
  }, []);

  const clearTargets = React.useCallback(() => {
    setTargets([]);
  }, []);

  const resetTargets = React.useCallback(() => {
    setTargets(DEFAULT_SKIP_TARGETS);
  }, []);

  return {
    targets,
    addTarget,
    removeTarget,
    updateTarget,
    clearTargets,
    resetTargets
  };
}