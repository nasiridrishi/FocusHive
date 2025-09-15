import React, { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { ResponsiveLayout } from '@shared/layout';
import { RouteLevelErrorBoundary } from '@shared/components/error-boundary';
import { SkipLink } from '@shared/components/SkipLink';
import { useAuth } from '../features/auth/hooks/useAuth';
import {
  LazyDashboardPage,
  LazyDiscoverPage,
  LazyErrorBoundaryDemo,
  LazyLoginPage,
  LazyRegisterPage,
  LazyPersonaSettingsPage,
  LazyPersonaManagementPage
} from './routes/LazyRoutes';
import { LazyGamificationDemo } from '@shared/components/lazy-features';

export function AuthenticatedApp(): React.ReactElement {
  const { authState } = useAuth();
  const { user, isAuthenticated } = authState;
  const navigate = useNavigate();
  
  // Persona state management
  const [currentPersona, setCurrentPersona] = useState<any>(null);

  // Debug logging
  console.log('AuthenticatedApp - user:', user);
  console.log('AuthenticatedApp - isAuthenticated:', isAuthenticated);

  // Map the authenticated user to the format expected by ResponsiveLayout
  const currentUser = user ? {
    name: user.name || user.displayName || user.username || user.email || 'User',
    email: user.email || 'user@example.com',
    avatar: user.avatar || undefined,
  } : undefined;

  console.log('AuthenticatedApp - currentUser:', currentUser);

  // Persona event handlers
  const handlePersonaSwitch = (persona: any) => {
    setCurrentPersona(persona);
    console.log('Switched to persona:', persona);
  };

  const handleCreatePersona = () => {
    console.log('handleCreatePersona called - navigating to /settings/personas/manage');
    // Add slight delay to ensure menu closes first
    setTimeout(() => {
      navigate('/settings/personas/manage');
      console.log('Navigation attempted');
    }, 100);
  };

  return (
    <>
      <SkipLink>Skip to main content</SkipLink>
      <ResponsiveLayout
        currentUser={currentUser}
        currentPersona={currentPersona}
        isConnected={isAuthenticated}
        notificationCount={0} // TODO: Get from notification context
        onPersonaSwitch={handlePersonaSwitch}
        onCreatePersona={handleCreatePersona}
      >
        <Routes>
          <Route
            path="/"
            element={<Navigate to="/dashboard" replace />}
          />
          <Route
            path="/login"
            element={
              <RouteLevelErrorBoundary routeName="Login">
                <LazyLoginPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/register"
            element={
              <RouteLevelErrorBoundary routeName="Register">
                <LazyRegisterPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/dashboard"
            element={
              <RouteLevelErrorBoundary routeName="Dashboard">
                <LazyDashboardPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/hives"
            element={
              <RouteLevelErrorBoundary routeName="Hives">
                <LazyDashboardPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/discover"
            element={
              <RouteLevelErrorBoundary routeName="Discover">
                <LazyDiscoverPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/gamification"
            element={
              <RouteLevelErrorBoundary routeName="Gamification">
                <LazyGamificationDemo />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/settings/personas"
            element={
              <RouteLevelErrorBoundary routeName="PersonaSettings">
                <LazyPersonaSettingsPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/settings/personas/manage"
            element={
              <RouteLevelErrorBoundary routeName="PersonaManagement">
                <LazyPersonaManagementPage />
              </RouteLevelErrorBoundary>
            }
          />
          <Route
            path="/error-boundary-demo"
            element={
              <RouteLevelErrorBoundary routeName="ErrorBoundaryDemo">
                <LazyErrorBoundaryDemo />
              </RouteLevelErrorBoundary>
            }
          />
        </Routes>
      </ResponsiveLayout>
    </>
  );
}