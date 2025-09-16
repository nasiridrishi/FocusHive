/**
 * Module declarations for optional external dependencies
 * These modules may or may not be installed depending on the deployment configuration
 */

type SentryUser = {
  id?: string;
  email?: string;
  username?: string;
  [key: string]: unknown;
};

type SentryContext = {
  user?: SentryUser;
  tags?: Record<string, string>;
  extra?: Record<string, unknown>;
  level?: 'error' | 'warning' | 'info' | 'debug';
  fingerprint?: string[];
};

type SentryBreadcrumb = {
  message: string;
  category?: string;
  level?: 'error' | 'warning' | 'info' | 'debug';
  data?: Record<string, unknown>;
};

type SentryOptions = {
  dsn: string;
  environment?: string;
  release?: string;
  integrations?: unknown[];
  tracesSampleRate?: number;
  beforeSend?: (event: unknown) => unknown | null;
};

declare module '@sentry/react' {
  export interface SentrySDK {
    init(options: SentryOptions): void;
    setUser(user: SentryUser | null): void;
    setTag(key: string, value: string): void;
    setContext(key: string, context: Record<string, unknown>): void;
    addBreadcrumb(breadcrumb: SentryBreadcrumb): void;
    captureException(error: Error, context?: SentryContext): string;
    captureMessage(message: string, level?: string, context?: SentryContext): string;
  }

  export function init(options: SentryOptions): void;
  export function setUser(user: SentryUser | null): void;
  export function setTag(key: string, value: string): void;
  export function setContext(key: string, context: Record<string, unknown>): void;
  export function addBreadcrumb(breadcrumb: SentryBreadcrumb): void;
  export function captureException(error: Error, context?: SentryContext): string;
  export function captureMessage(message: string, level?: string, context?: SentryContext): string;
}

type LogRocketOptions = {
  release?: string;
  console?: {
    shouldAggregateConsoleErrors?: boolean;
  };
  network?: {
    requestSanitizer?: (request: Record<string, unknown>) => Record<string, unknown>;
    responseSanitizer?: (response: Record<string, unknown>) => Record<string, unknown>;
  };
};

type LogRocketUser = {
  id?: string;
  email?: string;
  [key: string]: unknown;
};

declare module 'logrocket' {
  interface LogRocketSDK {
    init(appId: string, options?: LogRocketOptions): void;
    identify(userId: string, userInfo?: LogRocketUser): void;
    track(event: string, properties?: Record<string, unknown>): void;
    sessionURL: string;
    addTag(key: string, value: string): void;
  }

  const LogRocket: LogRocketSDK;
  export default LogRocket;
}
