export interface I18nConfig {
  defaultLanguage: string;
  supportedLanguages: string[];
  fallbackLanguage: string;
}

export const defaultI18nConfig: I18nConfig = {
  defaultLanguage: 'en',
  supportedLanguages: ['en', 'es', 'fr'],
  fallbackLanguage: 'en'
};

export function getLanguageFromNavigator(): string {
  if (typeof navigator !== 'undefined' && navigator.language) {
    const lang = navigator.language.split('-')[0];
    if (defaultI18nConfig.supportedLanguages.includes(lang)) {
      return lang;
    }
  }
  return defaultI18nConfig.defaultLanguage;
}

export function isLanguageSupported(lang: string): boolean {
  return defaultI18nConfig.supportedLanguages.includes(lang);
}