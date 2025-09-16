export {default as I18nProvider} from './I18nProvider'
export {withI18n, useI18nReady} from './i18nHooks'
export {
  default as LanguageSwitcher,
  CompactLanguageSwitcher,
  DetailedLanguageSwitcher
} from './LanguageSwitcher'

// Re-export hooks for convenience
export {
  useTranslation,
  useFormatting,
  useLanguageSwitcher,
  useTranslationWithValues,
  usePluralTranslation,
  useNumberFormatting,
  useDateFormatting,
  useErrorTranslation,
  useT,
  useFormat
} from '../../../hooks/useI18n'

// Re-export types
export type {
  LanguageSwitcherProps,
  SupportedLocale,
  TranslationKey,
  InterpolationValues,
  LanguageInfo,
  LocaleFormatOptions,
} from '../../../types/i18n'

// Re-export utilities
export {
  supportedLanguages,
  getCurrentLanguage,
  getAvailableLanguages,
  isRTL,
  changeLanguage,
  formatDate,
  formatNumber,
  formatCurrency,
  formatRelativeTime,
} from '../../../lib/i18n'