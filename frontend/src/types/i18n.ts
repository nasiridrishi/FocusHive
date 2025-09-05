import { defaultNS, resources } from '../lib/i18n'

// Type definitions for i18next
declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: typeof defaultNS
    resources: typeof resources['en']
  }
}

// Available namespaces
export type Namespace = keyof typeof resources['en']

// Translation keys for each namespace
export type CommonKeys = keyof typeof resources.en.common
export type AuthKeys = keyof typeof resources.en.auth
export type HiveKeys = keyof typeof resources.en.hive
export type TimerKeys = keyof typeof resources.en.timer
export type AnalyticsKeys = keyof typeof resources.en.analytics
export type GamificationKeys = keyof typeof resources.en.gamification
export type MusicKeys = keyof typeof resources.en.music
export type ChatKeys = keyof typeof resources.en.chat
export type PresenceKeys = keyof typeof resources.en.presence
export type ErrorKeys = keyof typeof resources.en.error
export type ValidationKeys = keyof typeof resources.en.validation

// Union type for all translation keys
export type TranslationKey = 
  | CommonKeys
  | `auth:${AuthKeys}`
  | `hive:${HiveKeys}`
  | `timer:${TimerKeys}`
  | `analytics:${AnalyticsKeys}`
  | `gamification:${GamificationKeys}`
  | `music:${MusicKeys}`
  | `chat:${ChatKeys}`
  | `presence:${PresenceKeys}`
  | `error:${ErrorKeys}`
  | `validation:${ValidationKeys}`

// Nested key types for type-safe translation access
export type NestedKeyOf<ObjectType extends Record<string, any>> = {
  [Key in keyof ObjectType & (string | number)]: ObjectType[Key] extends Record<string, any>
    ? `${Key}` | `${Key}.${NestedKeyOf<ObjectType[Key]>}`
    : `${Key}`
}[keyof ObjectType & (string | number)]

// Type-safe translation keys for each namespace
export type CommonTranslationKeys = NestedKeyOf<typeof resources.en.common>
export type AuthTranslationKeys = NestedKeyOf<typeof resources.en.auth>
export type HiveTranslationKeys = NestedKeyOf<typeof resources.en.hive>
export type TimerTranslationKeys = NestedKeyOf<typeof resources.en.timer>
export type AnalyticsTranslationKeys = NestedKeyOf<typeof resources.en.analytics>
export type GamificationTranslationKeys = NestedKeyOf<typeof resources.en.gamification>
export type MusicTranslationKeys = NestedKeyOf<typeof resources.en.music>
export type ChatTranslationKeys = NestedKeyOf<typeof resources.en.chat>
export type PresenceTranslationKeys = NestedKeyOf<typeof resources.en.presence>
export type ErrorTranslationKeys = NestedKeyOf<typeof resources.en.error>
export type ValidationTranslationKeys = NestedKeyOf<typeof resources.en.validation>

// Interpolation values type
export interface InterpolationValues {
  [key: string]: string | number | boolean | Date
}

// Language configuration type
export interface LanguageInfo {
  code: string
  name: string
  nativeName: string
  flag: string
  rtl: boolean
}

// Supported locales
export type SupportedLocale = 'en' | 'es' | 'fr'

// Date/time formatting options
export interface LocaleFormatOptions {
  date?: Intl.DateTimeFormatOptions
  time?: Intl.DateTimeFormatOptions
  number?: Intl.NumberFormatOptions
  currency?: Intl.NumberFormatOptions & { currency?: string }
  percent?: Intl.NumberFormatOptions
  relativeTime?: Intl.RelativeTimeFormatOptions
}

// Translation function with type safety
export interface TypedTFunction {
  // Default namespace (common)
  (key: CommonTranslationKeys, options?: InterpolationValues): string
  
  // Namespaced translations
  <T extends Namespace>(
    key: T extends 'auth' ? AuthTranslationKeys :
         T extends 'hive' ? HiveTranslationKeys :
         T extends 'timer' ? TimerTranslationKeys :
         T extends 'analytics' ? AnalyticsTranslationKeys :
         T extends 'gamification' ? GamificationTranslationKeys :
         T extends 'music' ? MusicTranslationKeys :
         T extends 'chat' ? ChatTranslationKeys :
         T extends 'presence' ? PresenceTranslationKeys :
         T extends 'error' ? ErrorTranslationKeys :
         T extends 'validation' ? ValidationTranslationKeys :
         string,
    options?: InterpolationValues & { ns?: T }
  ): string

  // With explicit namespace
  <T extends Namespace>(
    key: string,
    options: InterpolationValues & { ns: T }
  ): string
}

// Custom hook return types
export interface UseTranslationResult<T extends Namespace = 'common'> {
  t: TypedTFunction
  i18n: {
    language: SupportedLocale
    changeLanguage: (lng: SupportedLocale) => Promise<void>
    exists: (key: string, options?: { ns?: T }) => boolean
    getFixedT: (lng?: SupportedLocale, ns?: T) => TypedTFunction
    hasResourceBundle: (lng: SupportedLocale, ns: T) => boolean
    loadNamespaces: (ns: T | T[]) => Promise<void>
    loadLanguages: (lngs: SupportedLocale | SupportedLocale[]) => Promise<void>
    reloadResources: (lngs?: SupportedLocale | SupportedLocale[], ns?: T | T[]) => Promise<void>
  }
  ready: boolean
}

export interface UseFormattingResult {
  formatDate: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => string
  formatTime: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => string
  formatDateTime: (date: Date | string | number, options?: Intl.DateTimeFormatOptions) => string
  formatNumber: (number: number, options?: Intl.NumberFormatOptions) => string
  formatCurrency: (amount: number, currency?: string, options?: Intl.NumberFormatOptions) => string
  formatPercent: (value: number, options?: Intl.NumberFormatOptions) => string
  formatRelativeTime: (date: Date | string | number, options?: Intl.RelativeTimeFormatOptions) => string
  getCurrentLocale: () => SupportedLocale
  isRTL: () => boolean
}

// Language switcher props
export interface LanguageSwitcherProps {
  variant?: 'menu' | 'select' | 'buttons'
  size?: 'small' | 'medium' | 'large'
  showFlags?: boolean
  showNativeNames?: boolean
  onLanguageChange?: (language: SupportedLocale) => void
  className?: string
  disabled?: boolean
}

// RTL-aware component props
export interface RTLAwareProps {
  children: React.ReactNode
  className?: string
}

// Translation status for development
export interface TranslationStatus {
  namespace: Namespace
  locale: SupportedLocale
  total: number
  translated: number
  missing: string[]
  percentage: number
}

// Error types for translation system
export class TranslationError extends Error {
  constructor(
    message: string,
    public key: string,
    public namespace?: Namespace,
    public locale?: SupportedLocale
  ) {
    super(message)
    this.name = 'TranslationError'
  }
}

export class MissingTranslationError extends TranslationError {
  constructor(key: string, namespace?: Namespace, locale?: SupportedLocale) {
    super(`Missing translation for key: ${key}`, key, namespace, locale)
    this.name = 'MissingTranslationError'
  }
}

export class InvalidLocaleError extends Error {
  constructor(locale: string) {
    super(`Invalid locale: ${locale}. Supported locales: en, es, fr`)
    this.name = 'InvalidLocaleError'
  }
}

// Utility types for component props
export interface TranslatableComponentProps {
  titleKey?: CommonTranslationKeys
  descriptionKey?: CommonTranslationKeys
  labelKey?: CommonTranslationKeys
  placeholderKey?: CommonTranslationKeys
  tooltipKey?: CommonTranslationKeys
  interpolation?: InterpolationValues
}

// Form field translation props
export interface TranslatableFormFieldProps extends TranslatableComponentProps {
  errorKey?: ValidationTranslationKeys
  helperTextKey?: CommonTranslationKeys
  requiredMessage?: ValidationTranslationKeys
}

// Button translation props  
export interface TranslatableButtonProps extends TranslatableComponentProps {
  textKey?: CommonTranslationKeys
  loadingTextKey?: CommonTranslationKeys
  successTextKey?: CommonTranslationKeys
  errorTextKey?: CommonTranslationKeys
}