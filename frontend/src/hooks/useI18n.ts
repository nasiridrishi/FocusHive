import { useTranslation as useI18nextTranslation } from 'react-i18next'
import { useCallback, useMemo } from 'react'
import {
  formatDate as libFormatDate,
  formatNumber as libFormatNumber,
  formatCurrency as libFormatCurrency,
  formatRelativeTime as libFormatRelativeTime,
  getCurrentLanguage,
  isRTL,
  changeLanguage,
} from '../lib/i18n'
import type {
  Namespace,
  SupportedLocale,
  UseTranslationResult,
  UseFormattingResult,
  InterpolationValues,
  LocaleFormatOptions,
} from '../types/i18n'

/**
 * Enhanced useTranslation hook with type safety and additional features
 */
export function useTranslation<T extends Namespace = 'common'>(
  namespace?: T,
  options?: {
    keyPrefix?: string
    useSuspense?: boolean
  }
): UseTranslationResult<T> {
  const { t, i18n, ready } = useI18nextTranslation(namespace, options)

  const enhancedI18n = useMemo(() => ({
    ...i18n,
    language: i18n.language as SupportedLocale,
    changeLanguage: async (lng: SupportedLocale) => {
      await changeLanguage(lng)
      return i18n.changeLanguage(lng)
    },
  }), [i18n])

  return {
    t: t as unknown, // Type assertion needed due to i18next's complex typing
    i18n: enhancedI18n,
    ready,
  }
}

/**
 * Hook for locale-aware formatting functions
 */
export function useFormatting(): UseFormattingResult {
  const { i18n } = useI18nextTranslation()
  
  const formatDate = useCallback((
    date: Date | string | number,
    options?: Intl.DateTimeFormatOptions
  ): string => {
    return libFormatDate(date, options)
  }, [])

  const formatTime = useCallback((
    date: Date | string | number,
    options?: Intl.DateTimeFormatOptions
  ): string => {
    return libFormatDate(date, {
      hour: '2-digit',
      minute: '2-digit',
      ...options,
    })
  }, [])

  const formatDateTime = useCallback((
    date: Date | string | number,
    options?: Intl.DateTimeFormatOptions
  ): string => {
    return libFormatDate(date, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      ...options,
    })
  }, [])

  const formatNumber = useCallback((
    number: number,
    options?: Intl.NumberFormatOptions
  ): string => {
    return libFormatNumber(number, options)
  }, [])

  const formatCurrency = useCallback((
    amount: number,
    currency: string = 'USD',
    options?: Intl.NumberFormatOptions
  ): string => {
    return libFormatCurrency(amount, currency, options)
  }, [])

  const formatPercent = useCallback((
    value: number,
    options?: Intl.NumberFormatOptions
  ): string => {
    return libFormatNumber(value, {
      style: 'percent',
      ...options,
    })
  }, [])

  const formatRelativeTime = useCallback((
    date: Date | string | number,
    options?: Intl.RelativeTimeFormatOptions
  ): string => {
    return libFormatRelativeTime(date, options)
  }, [])

  const getCurrentLocale = useCallback((): SupportedLocale => {
    return i18n.language as SupportedLocale
  }, [i18n.language])

  const isCurrentRTL = useCallback((): boolean => {
    return isRTL()
  }, [])

  return {
    formatDate,
    formatTime,
    formatDateTime,
    formatNumber,
    formatCurrency,
    formatPercent,
    formatRelativeTime,
    getCurrentLocale,
    isRTL: isCurrentRTL,
  }
}

/**
 * Hook for language switching functionality
 */
export function useLanguageSwitcher() {
  const { i18n } = useI18nextTranslation()

  const currentLanguage = useMemo(() => getCurrentLanguage(), [])
  
  const switchLanguage = useCallback(async (language: SupportedLocale) => {
    // Language switch - let errors bubble up to caller
    await changeLanguage(language)
  }, [])

  const isLanguageLoading = useMemo(() => !i18n.isInitialized, [i18n.isInitialized])

  return {
    currentLanguage,
    switchLanguage,
    isLanguageLoading,
    isRTL: isRTL(),
  }
}

/**
 * Hook for translation with interpolation values
 */
export function useTranslationWithValues<T extends Namespace = 'common'>(
  namespace?: T
) {
  const { t } = useTranslation(namespace)

  const translate = useCallback((
    key: string,
    values?: InterpolationValues,
    options?: { count?: number; context?: string }
  ) => {
    return t(key, { ...values, ...options } as unknown)
  }, [t])

  return { t: translate }
}

/**
 * Hook for plural translations
 */
export function usePluralTranslation<T extends Namespace = 'common'>(
  namespace?: T
) {
  const { t } = useTranslation(namespace)

  const translatePlural = useCallback((
    key: string,
    count: number,
    values?: InterpolationValues
  ) => {
    return t(key, { count, ...values } as unknown)
  }, [t])

  return { translatePlural }
}

/**
 * Hook for locale-specific number formatting with custom options
 */
export function useNumberFormatting(options?: LocaleFormatOptions) {
  const { formatNumber, formatCurrency, formatPercent, getCurrentLocale } = useFormatting()

  const formatInteger = useCallback((value: number) => {
    return formatNumber(value, { 
      ...options?.number,
      maximumFractionDigits: 0 
    })
  }, [formatNumber, options?.number])

  const formatDecimal = useCallback((value: number, decimals: number = 2) => {
    return formatNumber(value, { 
      ...options?.number,
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    })
  }, [formatNumber, options?.number])

  const formatPrice = useCallback((amount: number, currency?: string) => {
    return formatCurrency(amount, currency, options?.currency)
  }, [formatCurrency, options?.currency])

  const formatPercentage = useCallback((value: number, decimals: number = 1) => {
    return formatPercent(value / 100, {
      ...options?.percent,
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    })
  }, [formatPercent, options?.percent])

  return {
    formatInteger,
    formatDecimal,
    formatPrice,
    formatPercentage,
    getCurrentLocale,
  }
}

/**
 * Hook for locale-specific date formatting with custom options
 */
export function useDateFormatting(options?: LocaleFormatOptions) {
  const { formatDate, formatTime, formatDateTime, formatRelativeTime, getCurrentLocale } = useFormatting()

  const formatShortDate = useCallback((date: Date | string | number) => {
    return formatDate(date, {
      ...options?.date,
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  }, [formatDate, options?.date])

  const formatLongDate = useCallback((date: Date | string | number) => {
    return formatDate(date, {
      ...options?.date,
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  }, [formatDate, options?.date])

  const formatShortTime = useCallback((date: Date | string | number) => {
    return formatTime(date, {
      ...options?.time,
      hour: '2-digit',
      minute: '2-digit'
    })
  }, [formatTime, options?.time])

  const formatFullDateTime = useCallback((date: Date | string | number) => {
    return formatDateTime(date, {
      ...options?.date,
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }, [formatDateTime, options?.date])

  const formatTimeAgo = useCallback((date: Date | string | number) => {
    return formatRelativeTime(date, {
      ...options?.relativeTime,
      numeric: 'auto',
      style: 'long'
    })
  }, [formatRelativeTime, options?.relativeTime])

  return {
    formatShortDate,
    formatLongDate,
    formatShortTime,
    formatFullDateTime,
    formatTimeAgo,
    getCurrentLocale,
  }
}

/**
 * Hook for error message translations
 */
export function useErrorTranslation() {
  const { t } = useTranslation('error')
  const { t: tValidation } = useTranslation('validation')

  const translateError = useCallback((
    error: Error | string,
    fallback?: string
  ): string => {
    const errorMessage = typeof error === 'string' ? error : error.message

    // Try to match common error patterns
    if (errorMessage.includes('network') || errorMessage.includes('fetch')) {
      return t('network_error')
    }
    if (errorMessage.includes('server') || errorMessage.includes('500')) {
      return t('server_error')
    }
    if (errorMessage.includes('404') || errorMessage.includes('not found')) {
      return t('page_not_found')
    }

    return fallback || t('something_went_wrong')
  }, [t])

  const translateValidationError = useCallback((
    field: string,
    errorType: string,
    values?: InterpolationValues
  ): string => {
    const key = `${errorType}` as unknown
    return tValidation(key, values)
  }, [tValidation])

  return {
    translateError,
    translateValidationError,
  }
}

// Re-export commonly used hooks for convenience
export { useTranslation as useT, useFormatting as useFormat }