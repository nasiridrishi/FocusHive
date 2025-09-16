import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './app/App'
import './index.css'
import 'focus-visible'
import {validateEnvironment} from './utils/envValidation'
import {logger} from './utils/logger'

// ========================================
// ENVIRONMENT VALIDATION
// ========================================

// Validate environment variables before application startup
// This ensures all required configuration is present and prevents
// runtime errors due to missing environment variables
try {
  logger.info('🚀 Starting FocusHive Frontend...');
  logger.info('🔍 Validating environment configuration...');

  // Validate and store environment configuration
  const env = validateEnvironment();

  // Make validated environment available globally for the application
  window.__FOCUSHIVE_ENV__ = env;

  logger.info('✅ Environment validation complete - starting React application');
} catch (error) {
  logger.error('❌ Environment validation failed:', error);

  // Show error page for environment configuration issues
  const errorMessage = error instanceof Error ? error.message : 'Unknown environment validation error';

  // Render error page instead of main app
  ReactDOM.createRoot(document.getElementById('root') || document.body).render(
      <div style={{
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
        padding: '2rem',
        maxWidth: '800px',
        margin: '0 auto',
        backgroundColor: '#fff',
        color: '#333'
      }}>
        <div style={{
          border: '2px solid #dc3545',
          borderRadius: '8px',
          padding: '2rem',
          backgroundColor: '#f8d7da'
        }}>
          <h1 style={{color: '#721c24', margin: '0 0 1rem 0'}}>
            ⚠️ Environment Configuration Error
          </h1>
          <div style={{
            backgroundColor: '#fff',
            border: '1px solid #dc3545',
            borderRadius: '4px',
            padding: '1rem',
            marginBottom: '1rem',
            fontFamily: 'monospace',
            fontSize: '14px',
            whiteSpace: 'pre-wrap',
            color: '#721c24'
          }}>
            {errorMessage}
          </div>
          <p style={{marginBottom: '1rem'}}>
            <strong>Quick Fix:</strong> Copy <code>.env.example</code> to <code>.env</code> and
            configure the required variables.
          </p>
          <p style={{margin: '0'}}>
            After fixing the configuration, refresh this page to continue.
          </p>
        </div>
      </div>
  );

  // Exit early to prevent the main application from loading
  throw error;
}

// ========================================
// REACT APPLICATION STARTUP
// ========================================

// PWA functionality is automatically initialized through PWAProvider in App.tsx
// Environment variables have been validated and are available via window.__FOCUSHIVE_ENV__

ReactDOM.createRoot(document.getElementById('root') || document.body).render(
    <React.StrictMode>
      <App/>
    </React.StrictMode>,
)