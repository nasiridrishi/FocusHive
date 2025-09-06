import React, { useEffect, useState } from 'react';
import i18n from '../../../lib/i18n';
import { I18nProvider } from './I18nProvider';

export function withI18n<P extends object>(
  Component: React.ComponentType<P>
): React.ComponentType<P> {
  const WrappedComponent = (props: P) => (
    <I18nProvider>
      <Component {...props} />
    </I18nProvider>
  )

  WrappedComponent.displayName = `withI18n(${Component.displayName || Component.name})`
  
  return WrappedComponent
}

export function useI18nReady() {
  const [isReady, setIsReady] = useState(i18n.isInitialized)

  useEffect(() => {
    if (i18n.isInitialized) {
      setIsReady(true)
      return
    }

    const checkReady = () => {
      if (i18n.isInitialized) {
        setIsReady(true)
      }
    }

    i18n.on('initialized', checkReady)
    i18n.on('loaded', checkReady)

    return () => {
      i18n.off('initialized', checkReady)
      i18n.off('loaded', checkReady)
    }
  }, [])

  return isReady
}