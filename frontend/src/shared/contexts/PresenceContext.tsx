import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react'
import { useWebSocket } from './WebSocketContext'
import { 
  PresenceContextType, 
  UserPresence, 
  PresenceStatus, 
  PresenceUpdate, 
  HivePresenceInfo, 
  FocusSession, 
  SessionBreak,
  ActivityEvent 
} from '../types/presence'

const PresenceContext = createContext<PresenceContextType | undefined>(undefined)

interface PresenceProviderProps {
  children: React.ReactNode
  userId: string
}

export const PresenceProvider: React.FC<PresenceProviderProps> = ({
  children,
  userId,
}) => {
  const { isConnected, emit, on } = useWebSocket()
  
  const [currentPresence, setCurrentPresence] = useState<UserPresence | null>(null)
  const [hivePresence, setHivePresence] = useState<Record<string, HivePresenceInfo>>({})
  const [currentSession, setCurrentSession] = useState<FocusSession | null>(null)
  
  // Use refs to track state for debouncing
  const lastStatusUpdate = useRef<Date>(new Date())
  const statusUpdateTimeoutRef = useRef<number | null>(null)
  const heartbeatIntervalRef = useRef<number | null>(null)

  // Event handlers - define first to avoid dependency issues
  const handlePresenceUpdate = useCallback((data: PresenceUpdate) => {
    setHivePresence(prev => {
      if (!data.hiveId) return prev

      const hiveInfo = prev[data.hiveId]
      if (!hiveInfo) return prev

      const updatedUsers = hiveInfo.activeUsers.map(user => 
        user.userId === data.userId 
          ? { ...user, status: data.status, currentActivity: data.activity, lastSeen: data.timestamp }
          : user
      )

      return {
        ...prev,
        [data.hiveId]: {
          ...hiveInfo,
          activeUsers: updatedUsers,
          totalOnline: updatedUsers.filter(u => u.status === 'online' || u.status === 'focusing').length,
          totalFocusing: updatedUsers.filter(u => u.status === 'focusing').length,
          totalOnBreak: updatedUsers.filter(u => u.status === 'break').length,
        }
      }
    })
  }, [])

  const handleHivePresenceInfo = useCallback((data: HivePresenceInfo) => {
    setHivePresence(prev => ({
      ...prev,
      [data.hiveId]: data
    }))
  }, [])

  const handleUserJoined = useCallback((data: { user: UserPresence, hiveId: string }) => {
    setHivePresence(prev => {
      const hiveInfo = prev[data.hiveId]
      if (!hiveInfo) return prev

      const userExists = hiveInfo.activeUsers.some(u => u.userId === data.user.userId)
      if (userExists) return prev

      const updatedUsers = [...hiveInfo.activeUsers, data.user]
      
      return {
        ...prev,
        [data.hiveId]: {
          ...hiveInfo,
          activeUsers: updatedUsers,
          totalOnline: updatedUsers.filter(u => u.status === 'online' || u.status === 'focusing').length,
          totalFocusing: updatedUsers.filter(u => u.status === 'focusing').length,
          totalOnBreak: updatedUsers.filter(u => u.status === 'break').length,
        }
      }
    })
  }, [])

  const handleUserLeft = useCallback((data: { userId: string, hiveId: string }) => {
    setHivePresence(prev => {
      const hiveInfo = prev[data.hiveId]
      if (!hiveInfo) return prev

      const updatedUsers = hiveInfo.activeUsers.filter(u => u.userId !== data.userId)
      
      return {
        ...prev,
        [data.hiveId]: {
          ...hiveInfo,
          activeUsers: updatedUsers,
          totalOnline: updatedUsers.filter(u => u.status === 'online' || u.status === 'focusing').length,
          totalFocusing: updatedUsers.filter(u => u.status === 'focusing').length,
          totalOnBreak: updatedUsers.filter(u => u.status === 'break').length,
        }
      }
    })
  }, [])

  const handleSessionStarted = useCallback((data: FocusSession) => {
    if (data.userId === userId) {
      setCurrentSession(data)
    }
  }, [userId])

  const handleSessionEnded = useCallback((data: { sessionId: string, userId: string }) => {
    if (data.userId === userId) {
      setCurrentSession(null)
    }
  }, [userId])

  const handleBreakStarted = useCallback((data: SessionBreak & { userId: string }) => {
    if (data.userId === userId) {
      // Update current session with break information
      setCurrentSession(prev => prev ? {
        ...prev,
        breaks: [...prev.breaks, data]
      } : null)
    }
  }, [userId])

  const handleBreakEnded = useCallback((data: { breakId: string, userId: string }) => {
    if (data.userId === userId) {
      // Update current session to mark break as ended
      setCurrentSession(prev => prev ? {
        ...prev,
        breaks: prev.breaks.map(b => 
          b.id === data.breakId 
            ? { ...b, isActive: false, endTime: new Date().toISOString() }
            : b
        )
      } : null)
    }
  }, [userId])

  const handleActivityEvent = useCallback((data: ActivityEvent) => {
    // Handle activity events for notifications or analytics
    void data; // Mark as intentionally used for future feature
  }, [])

  const sendStatusUpdate = useCallback((status: PresenceStatus, activity?: string) => {
    if (!isConnected || !userId) return

    const update: PresenceUpdate = {
      userId,
      status,
      activity,
      timestamp: new Date().toISOString(),
      hiveId: currentPresence?.hiveId,
    }

    emit('presence:update', update)
    
    setCurrentPresence(prev => prev ? {
      ...prev,
      status,
      currentActivity: activity,
      lastSeen: update.timestamp,
    } : null)
  }, [isConnected, userId, currentPresence?.hiveId, emit])

  const debouncedStatusUpdate = useCallback((status: PresenceStatus, activity?: string) => {
    const now = new Date()
    const timeSinceLastUpdate = now.getTime() - lastStatusUpdate.current.getTime()
    
    // Clear existing timeout
    if (statusUpdateTimeoutRef.current) {
      clearTimeout(statusUpdateTimeoutRef.current)
    }

    // If it's been more than 2 seconds since last update, send immediately
    if (timeSinceLastUpdate > 2000) {
      sendStatusUpdate(status, activity)
      lastStatusUpdate.current = now
    } else {
      // Otherwise, debounce for 1 second
      statusUpdateTimeoutRef.current = setTimeout(() => {
        sendStatusUpdate(status, activity)
        lastStatusUpdate.current = new Date()
      }, 1000) as unknown as number
    }
  }, [sendStatusUpdate])

  const updatePresence = useCallback((status: PresenceStatus, activity?: string) => {
    debouncedStatusUpdate(status, activity)
  }, [debouncedStatusUpdate])

  const initializePresence = useCallback(() => {
    if (!userId) return

    const initialPresence: UserPresence = {
      userId,
      user: { id: userId, name: '', username: '', email: '', firstName: '', lastName: '', isEmailVerified: false, createdAt: '', updatedAt: '' }, // Will be populated by backend
      status: 'online',
      lastSeen: new Date().toISOString(),
      deviceInfo: {
        type: 'web',
        browser: navigator.userAgent.includes('Chrome') ? 'Chrome' : 
                 navigator.userAgent.includes('Firefox') ? 'Firefox' : 'Other',
        os: navigator.platform,
      }
    }

    setCurrentPresence(initialPresence)
    emit('presence:initialize', { userId, presence: initialPresence })
  }, [userId, emit])

  const stopHeartbeat = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current)
      heartbeatIntervalRef.current = null
    }
  }, [])

  const startHeartbeat = useCallback(() => {
    if (heartbeatIntervalRef.current) return

    heartbeatIntervalRef.current = setInterval(() => {
      if (isConnected && currentPresence) {
        emit('presence:heartbeat', { 
          userId, 
          timestamp: new Date().toISOString() 
        })
      }
    }, 30000) as unknown as number // Send heartbeat every 30 seconds
  }, [isConnected, currentPresence, userId, emit])

  const joinHivePresence = useCallback((hiveId: string) => {
    if (!isConnected || !userId) return

    emit('presence:join_hive', { userId, hiveId })
    
    setCurrentPresence(prev => prev ? {
      ...prev,
      hiveId,
      lastSeen: new Date().toISOString(),
    } : null)
  }, [isConnected, userId, emit])

  const leaveHivePresence = useCallback((hiveId: string) => {
    if (!isConnected || !userId) return

    emit('presence:leave_hive', { userId, hiveId })
    
    setCurrentPresence(prev => prev ? {
      ...prev,
      hiveId: undefined,
      lastSeen: new Date().toISOString(),
    } : null)

    // Remove hive from local state
    setHivePresence(prev => {
      const updated = { ...prev }
      delete updated[hiveId]
      return updated
    })
  }, [isConnected, userId, emit])

  const startFocusSession = useCallback((hiveId?: string, targetDuration?: number) => {
    if (!isConnected || !userId) return

    const sessionData = {
      userId,
      hiveId: hiveId || currentPresence?.hiveId,
      targetDuration,
      type: targetDuration && targetDuration <= 30 ? 'pomodoro' : 'continuous',
      timestamp: new Date().toISOString(),
    }

    emit('session:start', sessionData)
    updatePresence('focusing', 'Started focus session')
  }, [isConnected, userId, currentPresence?.hiveId, emit, updatePresence])

  const endFocusSession = useCallback((productivity?: FocusSession['productivity']) => {
    if (!isConnected || !userId || !currentSession) return

    emit('session:end', { 
      userId, 
      sessionId: currentSession.id,
      productivity,
      timestamp: new Date().toISOString(),
    })
    
    updatePresence('online', 'Completed focus session')
    setCurrentSession(null)
  }, [isConnected, userId, currentSession, emit, updatePresence])

  const takeBreak = useCallback((type: SessionBreak['type']) => {
    if (!isConnected || !userId) return

    emit('session:break_start', { 
      userId, 
      sessionId: currentSession?.id,
      breakType: type,
      timestamp: new Date().toISOString(),
    })
    
    updatePresence('break', `Taking ${type} break`)
  }, [isConnected, userId, currentSession?.id, emit, updatePresence])

  const resumeFromBreak = useCallback(() => {
    if (!isConnected || !userId) return

    emit('session:break_end', { 
      userId, 
      sessionId: currentSession?.id,
      timestamp: new Date().toISOString(),
    })
    
    updatePresence('focusing', 'Resumed focus session')
  }, [isConnected, userId, currentSession?.id, emit, updatePresence])

  // Initialize presence when connected
  useEffect(() => {
    if (isConnected && userId) {
      initializePresence()
      startHeartbeat()
    }

    return () => {
      stopHeartbeat()
    }
  }, [isConnected, userId, initializePresence, startHeartbeat, stopHeartbeat])

  // Set up WebSocket event listeners
  useEffect(() => {
    if (!isConnected) return

    const unsubscribeFunctions = [
      on('presence:update', (data: unknown) => handlePresenceUpdate(data as PresenceUpdate)),
      on('presence:hive_info', (data: unknown) => handleHivePresenceInfo(data as HivePresenceInfo)),
      on('presence:user_joined', (data: unknown) => handleUserJoined(data as { user: UserPresence; hiveId: string })),
      on('presence:user_left', (data: unknown) => handleUserLeft(data as { userId: string; hiveId: string })),
      on('session:started', (data: unknown) => handleSessionStarted(data as FocusSession)),
      on('session:ended', (data: unknown) => handleSessionEnded(data as { sessionId: string; userId: string })),
      on('session:break_started', (data: unknown) => handleBreakStarted(data as SessionBreak & { userId: string })),
      on('session:break_ended', (data: unknown) => handleBreakEnded(data as { breakId: string; userId: string })),
      on('activity:event', (data: unknown) => handleActivityEvent(data as ActivityEvent)),
    ]

    return () => {
      unsubscribeFunctions.forEach(unsubscribe => unsubscribe())
    }
  }, [isConnected, on, handleActivityEvent, handleBreakEnded, handleBreakStarted, handleHivePresenceInfo, handlePresenceUpdate, handleSessionEnded, handleSessionStarted, handleUserJoined, handleUserLeft])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (statusUpdateTimeoutRef.current) {
        clearTimeout(statusUpdateTimeoutRef.current)
      }
      stopHeartbeat()
    }
  }, [stopHeartbeat])

  const value: PresenceContextType = {
    currentPresence,
    hivePresence,
    updatePresence,
    joinHivePresence,
    leaveHivePresence,
    startFocusSession,
    endFocusSession,
    takeBreak,
    resumeFromBreak,
  }

  return (
    <PresenceContext.Provider value={value}>
      {children}
    </PresenceContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const usePresence = (): PresenceContextType => {
  const context = useContext(PresenceContext)
  if (context === undefined) {
    throw new Error('usePresence must be used within a PresenceProvider')
  }
  return context
}

export default PresenceContext