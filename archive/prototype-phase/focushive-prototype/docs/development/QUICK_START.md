# FocusHive Quick Start Guide

## 🚀 Immediate Setup (30 minutes)

### 1. Initialize Project Structure
```bash
# Create monorepo structure
mkdir -p focushive-prototype/{client,server,shared}
cd focushive-prototype

# Initialize root package.json
npm init -y
npm install -D concurrently

# Update root package.json scripts
```

**Root package.json:**
```json
{
  "name": "focushive-prototype",
  "private": true,
  "scripts": {
    "dev": "concurrently \"npm run dev:server\" \"npm run dev:client\"",
    "dev:server": "cd server && npm run dev",
    "dev:client": "cd client && npm run dev",
    "build": "npm run build:shared && npm run build:server && npm run build:client",
    "build:shared": "cd shared && npm run build",
    "build:server": "cd server && npm run build",
    "build:client": "cd client && npm run build"
  },
  "devDependencies": {
    "concurrently": "^8.2.2"
  }
}
```

### 2. Setup Shared Types Package
```bash
cd shared
npm init -y
npm install -D typescript @types/node

# Create tsconfig.json
cat > tsconfig.json << 'EOF'
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "declaration": true,
    "declarationMap": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
EOF

# Create source files
mkdir src
touch src/types.ts src/constants.ts src/index.ts
```

### 3. Setup Server
```bash
cd ../server
npm init -y

# Install dependencies
npm install express cors dotenv socket.io bcryptjs jsonwebtoken
npm install -D @types/express @types/node @types/cors @types/bcryptjs @types/jsonwebtoken typescript ts-node nodemon @types/socket.io

# Create tsconfig.json
cat > tsconfig.json << 'EOF'
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true
  },
  "include": ["src/**/*"],
  "exclude": ["node_modules", "dist"]
}
EOF

# Create nodemon.json
cat > nodemon.json << 'EOF'
{
  "watch": ["src"],
  "ext": "ts",
  "exec": "ts-node src/index.ts"
}
EOF

# Update package.json scripts
npm pkg set scripts.dev="nodemon"
npm pkg set scripts.build="tsc"
npm pkg set scripts.start="node dist/index.js"

# Create source structure
mkdir -p src/{routes,services,middleware,socket,data,types,utils}
touch src/index.ts
```

### 4. Setup Client
```bash
cd ../client
npm create vite@latest . -- --template react-ts
npm install

# Install additional dependencies
npm install socket.io-client zustand react-router-dom axios clsx
npm install -D tailwindcss postcss autoprefixer @types/react-router-dom

# Initialize Tailwind
npx tailwindcss init -p

# Update tailwind.config.js
cat > tailwind.config.js << 'EOF'
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {},
  },
  plugins: [],
}
EOF

# Update src/index.css
cat > src/index.css << 'EOF'
@tailwind base;
@tailwind components;
@tailwind utilities;
EOF

# Create folder structure
mkdir -p src/{components,pages,contexts,hooks,services,types,utils,styles}
```

## 🎯 First Feature: Authentication (2 hours)

### Server: Create Basic Auth Service
```typescript
// server/src/services/authService.ts
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { User } from '../types';
import { dataStore } from '../data/store';

export class AuthService {
  async register(email: string, username: string, password: string): Promise<{ user: User; token: string }> {
    // Check if user exists
    const existingUser = dataStore.getUserByEmail(email);
    if (existingUser) {
      throw new Error('User already exists');
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(password, 10);

    // Create user
    const user: User = {
      id: `user_${Date.now()}`,
      email,
      username,
      password: hashedPassword,
      avatar: `https://ui-avatars.com/api/?name=${username}`,
      totalFocusTime: 0,
      currentStreak: 0,
      longestStreak: 0,
      points: 0,
      lastActiveDate: new Date().toISOString(),
      lookingForBuddy: false,
      preferences: {
        darkMode: false,
        soundEnabled: true,
        defaultPomodoro: {
          focusDuration: 25,
          breakDuration: 5
        }
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    dataStore.createUser(user);

    // Generate token
    const token = jwt.sign(
      { userId: user.id, email: user.email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: '7d' }
    );

    return { user, token };
  }

  async login(email: string, password: string): Promise<{ user: User; token: string }> {
    const user = dataStore.getUserByEmail(email);
    if (!user) {
      throw new Error('Invalid credentials');
    }

    const isValidPassword = await bcrypt.compare(password, user.password);
    if (!isValidPassword) {
      throw new Error('Invalid credentials');
    }

    const token = jwt.sign(
      { userId: user.id, email: user.email },
      process.env.JWT_SECRET || 'secret',
      { expiresIn: '7d' }
    );

    return { user, token };
  }
}

export const authService = new AuthService();
```

### Server: Create Auth Routes
```typescript
// server/src/routes/auth.routes.ts
import { Router } from 'express';
import { authService } from '../services/authService';

const router = Router();

router.post('/register', async (req, res) => {
  try {
    const { email, username, password } = req.body;
    const result = await authService.register(email, username, password);
    res.json(result);
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    const result = await authService.login(email, password);
    res.json(result);
  } catch (error: any) {
    res.status(401).json({ error: error.message });
  }
});

export default router;
```

### Client: Create Auth Context
```typescript
// client/src/contexts/AuthContext.tsx
import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';
import { User } from '../types';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      authService.getMe()
        .then(setUser)
        .catch(() => localStorage.removeItem('token'))
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  const login = async (email: string, password: string) => {
    const { user, token } = await authService.login(email, password);
    localStorage.setItem('token', token);
    setUser(user);
  };

  const register = async (email: string, username: string, password: string) => {
    const { user, token } = await authService.register(email, username, password);
    localStorage.setItem('token', token);
    setUser(user);
  };

  const logout = () => {
    localStorage.removeItem('token');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
```

### Client: Create Login Page
```typescript
// client/src/pages/Login.tsx
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 dark:text-white">
            Sign in to FocusHive
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600 dark:text-gray-400">
            Or{' '}
            <Link to="/register" className="font-medium text-blue-600 hover:text-blue-500">
              create a new account
            </Link>
          </p>
        </div>
        
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 dark:bg-red-900/50 text-red-600 dark:text-red-400 p-3 rounded-md text-sm">
              {error}
            </div>
          )}
          
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="email" className="sr-only">Email address</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
                placeholder="Email address"
              />
            </div>
            <div>
              <label htmlFor="password" className="sr-only">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
                placeholder="Password"
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {loading ? 'Signing in...' : 'Sign in'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
```

## 🏃‍♂️ Next Steps

1. **Complete Authentication Flow**
   - Create Register page
   - Add ProtectedRoute component
   - Implement logout functionality

2. **Setup Socket.io Connection**
   - Initialize Socket.io server
   - Create Socket context for client
   - Test real-time connection

3. **Build Dashboard**
   - Create stats overview
   - Implement room list
   - Add quick actions

4. **Implement Focus Room**
   - Build participant grid
   - Create timer component
   - Add real-time updates

## 📝 Development Tips

1. **Use TypeScript Strictly**
   ```typescript
   // Always define types
   interface Props {
     user: User;
     onUpdate: (user: User) => void;
   }
   ```

2. **Component Structure**
   ```typescript
   // components/common/Button.tsx
   export const Button: React.FC<ButtonProps> = ({ ...props }) => {
     // Implementation
   };
   ```

3. **Real-time Updates Pattern**
   ```typescript
   useEffect(() => {
     const handler = (data: any) => {
       // Handle update
     };
     
     socket.on('event', handler);
     return () => socket.off('event', handler);
   }, []);
   ```

4. **State Management**
   ```typescript
   // Use Zustand for global state
   const useAppStore = create((set) => ({
     // State and actions
   }));
   ```

## 🎯 Daily Goals

- **Day 1**: Auth + Basic Dashboard
- **Day 2**: Room Creation + Joining
- **Day 3**: Real-time Presence
- **Day 4**: Pomodoro Timer
- **Day 5**: Points & Gamification
- **Day 6**: Chat System
- **Day 7**: FocusBuddy
- **Day 8**: UI Polish
- **Day 9**: Testing & Fixes
- **Day 10**: Demo Prep

Start coding! 🚀