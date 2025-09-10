import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './app/App'
import './index.css'
import 'focus-visible'

// Note: Environment validation happens automatically when App renders
// The EnvironmentProvider will validate environment variables at startup
// and show an error page if required variables are missing.

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)