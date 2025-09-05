import React, { useEffect, useState } from 'react'
import { I18nextProvider } from 'react-i18next'
import { Box, CircularProgress, Alert, Typography } from '@mui/material'
import i18n, { isRTL } from '../../../lib/i18n'
import { useTranslation } from '../../../hooks/useI18n'

interface I18nProviderProps {
  children: React.ReactNode
  fallback?: React.ReactNode
  onLanguageChange?: (language: string) => void
}

/**
 * I18n Provider component that wraps the app with internationalization support
 */
export const I18nProvider: React.FC<I18nProviderProps> = ({
  children,
  fallback,
  onLanguageChange,
}) => {
  const [isReady, setIsReady] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const initializeI18n = async () => {
      try {
        // Wait for i18n to be ready
        await i18n.init()
        
        // Update document attributes for RTL support
        document.documentElement.lang = i18n.language
        document.documentElement.dir = isRTL() ? 'rtl' : 'ltr'
        
        setIsReady(true)
        onLanguageChange?.(i18n.language)
      } catch (err) {
        console.error('Failed to initialize i18n:', err)
        setError(err instanceof Error ? err.message : 'Failed to initialize i18n')
      }
    }

    // Listen for language changes
    const handleLanguageChange = (lng: string) => {
      // Update document attributes
      document.documentElement.lang = lng
      document.documentElement.dir = isRTL() ? 'rtl' : 'ltr'
      
      // Update body class for CSS styling
      document.body.classList.toggle('rtl', isRTL())
      document.body.classList.toggle('ltr', !isRTL())
      
      onLanguageChange?.(lng)
    }

    // Initialize if not already ready
    if (!i18n.isInitialized) {
      initializeI18n()
    } else {
      setIsReady(true)
    }

    // Subscribe to language changes
    i18n.on('languageChanged', handleLanguageChange)

    // Cleanup
    return () => {
      i18n.off('languageChanged', handleLanguageChange)
    }
  }, [onLanguageChange])

  // Show error state
  if (error) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        <Typography variant="h6" component="div">
          Internationalization Error
        </Typography>
        <Typography variant="body2">
          {error}
        </Typography>
      </Alert>
    )
  }

  // Show loading state
  if (!isReady) {
    return (
      fallback || (
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          minHeight="100vh"
          flexDirection="column"
          gap={2}
        >
          <CircularProgress size={40} />
          <Typography variant="body2" color="text.secondary">
            Loading translations...
          </Typography>
        </Box>
      )
    )
  }

  return (
    <I18nextProvider i18n={i18n}>
      <RTLProvider>
        {children}
      </RTLProvider>
    </I18nextProvider>
  )
}

/**
 * RTL Provider component that handles RTL/LTR layout changes
 */
interface RTLProviderProps {
  children: React.ReactNode
}

const RTLProvider: React.FC<RTLProviderProps> = ({ children }) => {
  const { i18n } = useTranslation()
  
  useEffect(() => {
    // Apply RTL/LTR styles to body
    document.body.classList.toggle('rtl', isRTL())
    document.body.classList.toggle('ltr', !isRTL())
    
    // Apply direction to root element
    document.documentElement.dir = isRTL() ? 'rtl' : 'ltr'
    
    // Apply language attribute
    document.documentElement.lang = i18n.language
  }, [i18n.language])

  return <>{children}</>
}

/**
 * Higher-order component for wrapping components with i18n support
 */
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

/**
 * Hook to get i18n readiness status
 */
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

export default I18nProvider