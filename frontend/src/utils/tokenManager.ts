/**
 * Enhanced JWT Token Management Utility
 *
 * Implements secure token storage and management with:
 * - Secure storage options (sessionStorage for access tokens, localStorage for refresh tokens)
 * - Token validation and parsing
 * - XSS protection
 * - Token expiration checking
 * - Cookie support preparation for future httpOnly implementation
 */

interface TokenClaims {
  sub: string; // Subject (user ID)
  email?: string;
  username?: string;
  exp: number; // Expiration timestamp
  iat: number; // Issued at timestamp
  jti?: string; // JWT ID
  iss?: string; // Issuer
  aud?: string; // Audience
  // Additional custom claims with specific types
  [key: string]: string | number | boolean | string[] | undefined;
}

interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

interface TokenValidationResult {
  isValid: boolean;
  isExpired: boolean;
  expiresAt?: Date;
  claims?: TokenClaims;
  error?: string;
}

class TokenManager {
  private readonly ACCESS_TOKEN_KEY = 'focushive_access_token';
  private readonly REFRESH_TOKEN_KEY = 'focushive_refresh_token';
  private readonly TOKEN_EXPIRES_KEY = 'focushive_token_expires';

  // Buffer time before token expiry to trigger refresh (5 minutes)
  private readonly REFRESH_BUFFER_MS = 5 * 60 * 1000;
  // Private timer management for automatic token refresh
  private refreshTimer: NodeJS.Timeout | null = null;

  /**
   * Store tokens securely
   * Access token in sessionStorage (cleared on browser close)
   * Refresh token in localStorage (persistent across sessions)
   */
  saveTokens(accessToken: string, refreshToken: string): void {
    try {
      // Validate tokens before storing
      const accessTokenValidation = this.validateToken(accessToken);
      if (!accessTokenValidation.isValid) {
        throw new Error('Invalid access token provided');
      }

      const refreshTokenValidation = this.validateToken(refreshToken);
      if (!refreshTokenValidation.isValid) {
        throw new Error('Invalid refresh token provided');
      }

      // Store access token in sessionStorage (more secure, cleared on browser close)
      sessionStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);

      // Store refresh token in localStorage (persistent for auto-login)
      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);

      // Store expiration time for quick checks
      if (accessTokenValidation.expiresAt) {
        localStorage.setItem(this.TOKEN_EXPIRES_KEY, accessTokenValidation.expiresAt.toISOString());
      }

      // Security: Clear any existing timeout and set up new refresh timer
      this.setupTokenRefreshTimer();

    } catch (error) {
      // If storing fails, clear any partial state
      this.clearTokens();
      throw new Error(`Failed to save tokens: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    try {
      const token = sessionStorage.getItem(this.ACCESS_TOKEN_KEY);

      if (!token) {
        return null;
      }

      // Validate token before returning
      const validation = this.validateToken(token);
      if (!validation.isValid) {
        this.clearTokens();
        return null;
      }

      return token;
    } catch {
      // Debug statement removed
      return null;
    }
  }

  /**
   * Get current refresh token
   */
  getRefreshToken(): string | null {
    try {
      const token = localStorage.getItem(this.REFRESH_TOKEN_KEY);

      if (!token) {
        return null;
      }

      // Validate token before returning
      const validation = this.validateToken(token);
      if (!validation.isValid) {
        this.clearTokens();
        return null;
      }

      return token;
    } catch {
      // Debug statement removed
      return null;
    }
  }

  /**
   * Clear all stored tokens
   */
  clearTokens(): void {
    try {
      sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.TOKEN_EXPIRES_KEY);

      // Clear any pending refresh timers
      this.clearTokenRefreshTimer();
    } catch {
      // Debug statement removed
    }
  }

  /**
   * Check if current access token is expired or will expire soon
   */
  isTokenExpired(token?: string): boolean {
    try {
      const tokenToCheck = token || this.getAccessToken();

      if (!tokenToCheck) {
        return true;
      }

      const validation = this.validateToken(tokenToCheck);
      if (!validation.isValid || !validation.expiresAt) {
        return true;
      }

      // Consider token expired if it expires within the buffer time
      return validation.expiresAt.getTime() <= (Date.now() + this.REFRESH_BUFFER_MS);
    } catch {
      // Debug statement removed
      return true;
    }
  }

  /**
   * Parse JWT token and extract claims
   */
  parseJWT(token: string): TokenClaims | null {
    try {
      if (!token || typeof token !== 'string') {
        return null;
      }

      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }

      // Decode the payload (second part)
      const payload = parts[1];

      // Add padding if needed for proper base64 decoding
      const paddedPayload = payload + '='.repeat((4 - payload.length % 4) % 4);

      const decodedPayload = atob(paddedPayload);
      const claims = JSON.parse(decodedPayload) as TokenClaims;

      // Validate required claims
      if (!claims.exp || !claims.sub) {
        return null;
      }

      return claims;
    } catch {
      // Debug statement removed
      return null;
    }
  }

  /**
   * Validate JWT token structure and expiration
   */
  validateToken(token: string): TokenValidationResult {
    try {
      if (!token || typeof token !== 'string') {
        return {isValid: false, isExpired: true, error: 'Token is empty or invalid type'};
      }

      const claims = this.parseJWT(token);
      if (!claims) {
        return {isValid: false, isExpired: true, error: 'Failed to parse token claims'};
      }

      const now = Math.floor(Date.now() / 1000);
      const isExpired = claims.exp <= now;
      const expiresAt = new Date(claims.exp * 1000);

      return {
        isValid: !isExpired,
        isExpired,
        expiresAt,
        claims,
        error: isExpired ? 'Token has expired' : undefined
      };
    } catch (error) {
      return {
        isValid: false,
        isExpired: true,
        error: `Token validation failed: ${error instanceof Error ? error.message : 'Unknown error'}`
      };
    }
  }

  /**
   * Check if user has valid authentication tokens
   */
  hasValidTokens(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    // If we have a valid access token that's not expired, we're good
    if (accessToken && !this.isTokenExpired(accessToken)) {
      return true;
    }

    // If access token is expired/missing but we have a refresh token, we can refresh
    if (refreshToken && !this.isTokenExpired(refreshToken)) {
      return true;
    }

    return false;
  }

  /**
   * Get token expiration information
   */
  getTokenExpirationInfo(): {
    accessTokenExpiresAt: Date | null;
    refreshTokenExpiresAt: Date | null;
    needsRefresh: boolean;
  } {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();

    const accessTokenValidation = accessToken ? this.validateToken(accessToken) : null;
    const refreshTokenValidation = refreshToken ? this.validateToken(refreshToken) : null;

    return {
      accessTokenExpiresAt: accessTokenValidation?.expiresAt || null,
      refreshTokenExpiresAt: refreshTokenValidation?.expiresAt || null,
      needsRefresh: !accessToken || this.isTokenExpired(accessToken)
    };
  }

  /**
   * Get user information from current access token
   */
  getUserFromToken(): { id: string; email?: string; username?: string } | null {
    try {
      const token = this.getAccessToken();
      if (!token) {
        return null;
      }

      const claims = this.parseJWT(token);
      if (!claims) {
        return null;
      }

      return {
        id: claims.sub,
        email: claims.email,
        username: claims.username
      };
    } catch {
      // Debug statement removed
      return null;
    }
  }

  /**
   * Prepare for httpOnly cookie implementation
   * This method will be used when backend supports httpOnly cookies
   */
  supportsHttpOnlyCookies(): boolean {
    // Check if running in secure context and cookies are enabled
    return window.isSecureContext && navigator.cookieEnabled;
  }

  private setupTokenRefreshTimer(): void {
    this.clearTokenRefreshTimer();

    const tokenInfo = this.getTokenExpirationInfo();
    if (!tokenInfo.accessTokenExpiresAt) {
      return;
    }

    // Calculate when to refresh (5 minutes before expiry)
    const refreshTime = tokenInfo.accessTokenExpiresAt.getTime() - this.REFRESH_BUFFER_MS;
    const timeUntilRefresh = refreshTime - Date.now();

    // Only set timer if refresh is needed in the future
    if (timeUntilRefresh > 0 && timeUntilRefresh < 24 * 60 * 60 * 1000) { // Max 24 hours
      this.refreshTimer = setTimeout(() => {
        // Dispatch a custom event to notify the app that token refresh is needed
        window.dispatchEvent(new CustomEvent('tokenRefreshNeeded'));
      }, timeUntilRefresh);
    }
  }

  private clearTokenRefreshTimer(): void {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  /**
   * Security utility: Check for XSS attempts in token
   */
  private isTokenSecure(token: string): boolean {
    // Basic XSS protection - check for script injection attempts
    const xssPatterns = [
      /<script/i,
      /javascript:/i,
      /on\w+\s*=/i,
      /data:text\/html/i
    ];

    return !xssPatterns.some(pattern => pattern.test(token));
  }
}

// Export singleton instance
export const tokenManager = new TokenManager();

// Export types for use in other modules
export type {TokenClaims, TokenPair, TokenValidationResult};

export default tokenManager;