import {SvgIconProps} from '@mui/material';
import {ComponentType, useEffect, useState} from 'react';

export interface DynamicIconProps extends SvgIconProps {
  /**
   * Name of the Material-UI icon (without the 'Icon' suffix)
   * e.g., 'Add', 'Delete', 'Star', etc.
   */
  name: string

  /**
   * Fallback icon to show while loading or if icon fails to load
   */
  fallback?: ComponentType<SvgIconProps>

  /**
   * Show loading indicator while icon loads
   */
  showLoading?: boolean
}

// Icon loading cache to prevent multiple loads
type IconModule = { default?: React.ElementType; [key: string]: React.ElementType | undefined };
const iconCache = new Map<string, Promise<IconModule | null>>();

export const preloadCommonIcons = (): void => {
  const commonIcons = [
    'Home', 'Dashboard', 'Settings', 'Person', 'Notifications',
    'Search', 'Menu', 'Close', 'Add', 'Remove', 'Edit', 'Delete'
  ];

  commonIcons.forEach(name => {
    loadIcon(name);
  });
};

function loadIcon(name: string): Promise<IconModule | null> {
  if (!iconCache.has(name)) {
    iconCache.set(name, import(`@mui/icons-material/${name}`).catch(() => null));
  }
  return iconCache.get(name) ?? null;
}

export function useDynamicIcon(name: string): Record<string, unknown> {
  const [Icon, setIcon] = useState<React.ElementType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    setLoading(true);
    setError(null);

    loadIcon(name)
    .then((module) => {
      if (!cancelled && module) {
        setIcon(() => module.default || module[name]);
      } else if (!cancelled) {
        setError(new Error(`Icon ${name} not found`));
      }
    })
    .catch((err) => {
      if (!cancelled) {
        setError(err);
      }
    })
    .finally(() => {
      if (!cancelled) {
        setLoading(false);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [name]);

  return {Icon, loading, error};
}