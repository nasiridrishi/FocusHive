import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { SocketProvider } from './contexts/SocketContext';
import { GamificationProvider } from './contexts/GamificationContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { ToastProvider } from './contexts/ToastContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login, Register, Dashboard } from './pages';
import { Room } from './pages/Room';
import { RoomV3 } from './pages/RoomV3';
import { Forums } from './pages/Forums';
import { AchievementPopup } from './components/AchievementPopup';
import { ToastContainer } from './components/ui/Toast';

function App() {
  return (
    <Router>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>
            <SocketProvider>
              <GamificationProvider>
                <ToastContainer />
                <AchievementPopup />
                <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route
                path="/forums"
                element={
                  <ProtectedRoute>
                    <Forums />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <Dashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/room/:roomId"
                element={
                  <ProtectedRoute>
                    <RoomV3 />
                  </ProtectedRoute>
                }
              />
              <Route path="/" element={<Navigate to="/forums" replace />} />
            </Routes>
          </GamificationProvider>
        </SocketProvider>
      </AuthProvider>
      </ToastProvider>
    </ThemeProvider>
    </Router>
  );
}

export default App;