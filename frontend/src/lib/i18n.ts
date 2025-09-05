import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

// Import translation resources
import enCommon from '../locales/en/common.json'
import enAuth from '../locales/en/auth.json'
import enHive from '../locales/en/hive.json'
import enTimer from '../locales/en/timer.json'
import enAnalytics from '../locales/en/analytics.json'
import enGamification from '../locales/en/gamification.json'
import enMusic from '../locales/en/music.json'
import enChat from '../locales/en/chat.json'
import enPresence from '../locales/en/presence.json'
import enError from '../locales/en/error.json'
import enValidation from '../locales/en/validation.json'

import esCommon from '../locales/es/common.json'
import esAuth from '../locales/es/auth.json'
import esHive from '../locales/es/hive.json'
import esTimer from '../locales/es/timer.json'
import esAnalytics from '../locales/es/analytics.json'
import esGamification from '../locales/es/gamification.json'
import esMusic from '../locales/es/music.json'
import esChat from '../locales/es/chat.json'
import esPresence from '../locales/es/presence.json'
import esError from '../locales/es/error.json'
import esValidation from '../locales/es/validation.json'

import frCommon from '../locales/fr/common.json'
import frAuth from '../locales/fr/auth.json'
import frHive from '../locales/fr/hive.json'
import frTimer from '../locales/fr/timer.json'
import frAnalytics from '../locales/fr/analytics.json'
import frGamification from '../locales/fr/gamification.json'
import frMusic from '../locales/fr/music.json'
import frChat from '../locales/fr/chat.json'
import frPresence from '../locales/fr/presence.json'
import frError from '../locales/fr/error.json'
import frValidation from '../locales/fr/validation.json'

// Language resources organized by namespace
export const resources = {
  en: {
    common: enCommon,
    auth: enAuth,
    hive: enHive,
    timer: enTimer,
    analytics: enAnalytics,
    gamification: enGamification,
    music: enMusic,
    chat: enChat,
    presence: enPresence,
    error: enError,
    validation: enValidation,
  },
  es: {
    common: esCommon,
    auth: esAuth,
    hive: esHive,
    timer: esTimer,
    analytics: esAnalytics,
    gamification: esGamification,
    music: esMusic,
    chat: esChat,
    presence: esPresence,
    error: esError,
    validation: esValidation,
  },
  fr: {
    common: frCommon,
    auth: frAuth,
    hive: frHive,
    timer: frTimer,
    analytics: frAnalytics,
    gamification: frGamification,
    music: frMusic,
    chat: frChat,
    presence: frPresence,
    error: frError,
    validation: frValidation,
  },
} as const

// Supported languages configuration
export const supportedLanguages = {
  en: { 
    code: 'en', 
    name: 'English', 
    nativeName: 'English', 
    flag: '🇺🇸',
    rtl: false 
  },
  es: { 
    code: 'es', 
    name: 'Spanish', 
    nativeName: 'Español', 
    flag: '🇪🇸',
    rtl: false 
  },
  fr: { 
    code: 'fr', 
    name: 'French', 
    nativeName: 'Français', 
    flag: '🇫🇷',
    rtl: false 
  },
} as const

export type SupportedLanguage = keyof typeof supportedLanguages
export type LanguageConfig = typeof supportedLanguages[SupportedLanguage]

// RTL languages (for future expansion)
export const rtlLanguages: SupportedLanguage[] = []

// Default namespace
export const defaultNS = 'common'

// Configure i18next
i18n
  // Detect user language
  .use(LanguageDetector)
  // Pass the i18n instance to react-i18next
  .use(initReactI18next)
  // Initialize i18next
  .init({
    // Debug mode for development
    debug: import.meta.env.DEV || false,

    // Fallback language
    fallbackLng: 'en',

    // Default namespace
    defaultNS,

    // Available namespaces
    ns: [
      'common',
      'auth',
      'hive',
      'timer',
      'analytics',
      'gamification',
      'music',
      'chat',
      'presence',
      'error',
      'validation',
    ],

    // Resources
    resources,

    // Language detection options
    detection: {
      // Order and from where user language should be detected
      order: ['querystring', 'localStorage', 'navigator', 'htmlTag', 'path', 'subdomain'],

      // Keys or params to lookup language from
      lookupQuerystring: 'lng',
      lookupLocalStorage: 'focushive-language',
      lookupFromPathIndex: 0,
      lookupFromSubdomainIndex: 0,

      // Cache user language on
      caches: ['localStorage'],

      // Optional expire and domain for set cookie
      cookieMinutes: 10080, // 7 days
      cookieDomain: 'focushive.com',

      // Optional htmlTag with lang attribute
      htmlTag: document.documentElement,
      checkWhitelist: true,
    },

    // Interpolation options
    interpolation: {
      // React already escapes by default
      escapeValue: false,

      // Format function for interpolation
      format: (value, format, lng) => {
        if (format === 'uppercase') return value.toUpperCase()
        if (format === 'lowercase') return value.toLowerCase()
        if (format === 'capitalize') return value.charAt(0).toUpperCase() + value.slice(1)
        
        // Date formatting
        if (format === 'date') {
          return new Intl.DateTimeFormat(lng).format(new Date(value))
        }
        if (format === 'datetime') {
          return new Intl.DateTimeFormat(lng, {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          }).format(new Date(value))
        }
        if (format === 'time') {
          return new Intl.DateTimeFormat(lng, {
            hour: '2-digit',
            minute: '2-digit',
          }).format(new Date(value))
        }

        // Number formatting
        if (format === 'number') {
          return new Intl.NumberFormat(lng).format(value)
        }
        if (format === 'currency') {
          return new Intl.NumberFormat(lng, {
            style: 'currency',
            currency: 'USD', // Default currency, can be made configurable
          }).format(value)
        }
        if (format === 'percent') {
          return new Intl.NumberFormat(lng, {
            style: 'percent',
          }).format(value / 100)
        }

        return value
      },
    },

    // React specific options
    react: {
      // Turn off the use of React Suspense for i18n loading
      useSuspense: false,
      
      // Bind i18n instance to React component tree
      bindI18n: 'languageChanged',
      
      // Bind store to React component tree
      bindI18nStore: 'added removed',
      
      // Enable or disable the TransWithoutContext hoc
      transEmptyNodeValue: '',
      
      // Trans component key validation
      transSupportBasicHtmlNodes: true,
      transKeepBasicHtmlNodesFor: ['br', 'strong', 'i'],
      
      // Escape passed in values to avoid XSS injection
      escapeValue: true,

      // Set to false if you prefer not to use the default Trans component
      defaultTransParent: 'div',

      // You can choose to only update when i18n language changes or also when
      // the loaded resources change
    },

    // Backend options (if using i18next-http-backend)
    // backend: {
    //   loadPath: '/locales/{{lng}}/{{ns}}.json',
    //   allowMultiLoading: false,
    //   parse: (data: string) => JSON.parse(data),
    //   crossDomain: false,
    //   addPath: '/locales/add/{{lng}}/{{ns}}',
    //   allowMultiLoading: false,
    //   reloadInterval: false,
    // },

    // Separators
    keySeparator: '.',
    nsSeparator: ':',

    // Pluralization
    pluralSeparator: '_',
    contextSeparator: '_',

    // Postprocessing
    postProcess: ['interval'],

    // Cleanup
    cleanCode: true,
    
    // Additional options for TypeScript support
    // This helps with type inference
    returnEmptyString: false,
    returnNull: false,
    returnObjects: false,
    joinArrays: false,

    // Savemissing feature
    saveMissing: import.meta.env.DEV,
    saveMissingTo: 'all',
    missingKeyHandler: import.meta.env.DEV 
      ? (_lng: string[], _ns: string, _key: string) => {
          // Missing translation logged only in development
        }
      : undefined,
  } as unknown)

export default i18n

// Helper function to get current language config
export const getCurrentLanguage = (): LanguageConfig => {
  const currentLang = i18n.language as SupportedLanguage
  return supportedLanguages[currentLang] || supportedLanguages.en
}

// Helper function to check if current language is RTL
export const isRTL = (): boolean => {
  const currentLang = i18n.language as SupportedLanguage
  return rtlLanguages.includes(currentLang)
}

// Helper function to get available languages for language switcher
export const getAvailableLanguages = () => {
  return Object.values(supportedLanguages)
}

// Helper function to change language
export const changeLanguage = async (language: SupportedLanguage): Promise<void> => {
  await i18n.changeLanguage(language)
  
  // Update HTML lang attribute for accessibility
  document.documentElement.lang = language
  
  // Update HTML dir attribute for RTL support
  document.documentElement.dir = rtlLanguages.includes(language) ? 'rtl' : 'ltr'
  
  // Store in localStorage for persistence
  localStorage.setItem('focushive-language', language)
}

// Helper function to format dates with current locale
export const formatDate = (
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string => {
  const currentLang = i18n.language
  return new Intl.DateTimeFormat(currentLang, options).format(new Date(date))
}

// Helper function to format numbers with current locale
export const formatNumber = (
  number: number,
  options?: Intl.NumberFormatOptions
): string => {
  const currentLang = i18n.language
  return new Intl.NumberFormat(currentLang, options).format(number)
}

// Helper function to format currency with current locale
export const formatCurrency = (
  amount: number,
  currency: string = 'USD',
  options?: Intl.NumberFormatOptions
): string => {
  const currentLang = i18n.language
  return new Intl.NumberFormat(currentLang, {
    style: 'currency',
    currency,
    ...options,
  }).format(amount)
}

// Helper function to format relative time
export const formatRelativeTime = (
  date: Date | string | number,
  options?: Intl.RelativeTimeFormatOptions
): string => {
  const currentLang = i18n.language
  const rtf = new Intl.RelativeTimeFormat(currentLang, options)
  
  const now = new Date()
  const target = new Date(date)
  const diffInSeconds = Math.floor((target.getTime() - now.getTime()) / 1000)
  
  // Convert to appropriate unit
  if (Math.abs(diffInSeconds) < 60) {
    return rtf.format(diffInSeconds, 'second')
  } else if (Math.abs(diffInSeconds) < 3600) {
    return rtf.format(Math.floor(diffInSeconds / 60), 'minute')
  } else if (Math.abs(diffInSeconds) < 86400) {
    return rtf.format(Math.floor(diffInSeconds / 3600), 'hour')
  } else {
    return rtf.format(Math.floor(diffInSeconds / 86400), 'day')
  }
}