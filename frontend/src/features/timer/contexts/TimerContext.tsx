import React, { createContext, useContext, useEffect, useState, useCallback, useRef } from 'react'
import { useWebSocket } from '../../../shared/contexts/WebSocketContext'
import { usePresence } from '../../../shared/contexts/PresenceContext'
import { 
  TimerContextType, 
  TimerState, 
  TimerSettings, 
  SessionStats, 
  SessionGoal,
 
} from '../../../shared/types/timer'

const TimerContext = createContext<TimerContextType | undefined>(undefined)

interface TimerProviderProps {
  children: React.ReactNode
  userId: string
}

// Default timer settings following Pomodoro technique
const DEFAULT_TIMER_SETTINGS: TimerSettings = {
  focusLength: 25,
  shortBreakLength: 5,
  longBreakLength: 15,
  longBreakInterval: 4,
  autoStartBreaks: false,
  autoStartFocus: false,
  soundEnabled: true,
  notificationsEnabled: true,
  selectedSounds: {
    focusStart: 'bell',
    focusEnd: 'chime',
    breakStart: 'gentle',
    breakEnd: 'ready'
  }
}

const DEFAULT_TIMER_STATE: TimerState = {
  currentPhase: 'idle',
  timeRemaining: 0,
  isRunning: false,
  isPaused: false,
  currentCycle: 0,
  totalCycles: 0,
}

export const TimerProvider: React.FC<TimerProviderProps> = ({
  children,
  userId,
}) => {
  const { isConnected, emit, on } = useWebSocket()
  const { updatePresence, currentPresence } = usePresence()
  
  const [timerState, setTimerState] = useState<TimerState>(DEFAULT_TIMER_STATE)
  const [timerSettings, setTimerSettings] = useState<TimerSettings>(DEFAULT_TIMER_SETTINGS)
  const [currentSession, setCurrentSession] = useState<SessionStats | null>(null)
  
  // Refs for timer management
  const timerIntervalRef = useRef<number | null>(null)
  const phaseStartTimeRef = useRef<Date | null>(null)
  const sessionStartTimeRef = useRef<Date | null>(null)
  const audioContextRef = useRef<AudioContext | null>(null)
  
  // Load settings from localStorage on mount
  useEffect(() => {
    const savedSettings = localStorage.getItem(`timer-settings-${userId}`)
    if (savedSettings) {
      try {
        const parsed = JSON.parse(savedSettings)
        setTimerSettings({ ...DEFAULT_TIMER_SETTINGS, ...parsed })
      } catch (error) {
        console.error('Failed to parse saved timer settings:', error)
      }
    }
  }, [userId])

  // Save settings to localStorage when they change
  useEffect(() => {
    localStorage.setItem(`timer-settings-${userId}`, JSON.stringify(timerSettings))
  }, [timerSettings, userId])

  // Utility functions
  const generateSessionId = useCallback((): string => {
    return `session_${userId}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }, [userId])

  const generateGoalId = useCallback((): string => {
    return `goal_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }, [])

  // Sound notification helper
  const playNotificationSound = useCallback((soundType: string) => {
    if (!timerSettings.soundEnabled || !audioContextRef.current) return
    
    // Use soundType to determine frequency
    const frequency = soundType === 'break' ? 440 : 880
    
    try {
      // Create a simple beep sound
      const oscillator = audioContextRef.current.createOscillator()
      const gainNode = audioContextRef.current.createGain()
      
      oscillator.connect(gainNode)
      gainNode.connect(audioContextRef.current.destination)
      
      oscillator.frequency.setValueAtTime(frequency, audioContextRef.current.currentTime)
      gainNode.gain.setValueAtTime(0.1, audioContextRef.current.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.001, audioContextRef.current.currentTime + 0.5)
      
      oscillator.start(audioContextRef.current.currentTime)
      oscillator.stop(audioContextRef.current.currentTime + 0.5)
    } catch (error) {
      console.warn('Failed to play sound:', error)
    }
  }, [timerSettings.soundEnabled])

  // Browser notification helper
  const showNotification = useCallback((phase: TimerState['currentPhase']) => {
    if (!timerSettings.notificationsEnabled) return
    
    if ('Notification' in window && Notification.permission === 'granted') {
      const title = phase === 'focus' ? 'Focus Time Complete!' : 'Break Time Complete!'
      const body = phase === 'focus' 
        ? 'Great work! Time for a break.' 
        : 'Break\'s over. Ready to focus?'
      
      new Notification(title, {
        body,
        icon: '/logo192.png',
        tag: 'focushive-timer'
      })
    }
  }, [timerSettings.notificationsEnabled])

  const getPhaseLength = useCallback((phase: TimerState['currentPhase']): number => {
    switch (phase) {
      case 'focus': return timerSettings.focusLength
      case 'short-break': return timerSettings.shortBreakLength
      case 'long-break': return timerSettings.longBreakLength
      default: return 0
    }
  }, [timerSettings])

  const getNextPhase = useCallback((state: TimerState): TimerState['currentPhase'] => {
    if (state.currentPhase === 'focus') {
      const shouldTakeLongBreak = (state.currentCycle + 1) % timerSettings.longBreakInterval === 0
      return shouldTakeLongBreak ? 'long-break' : 'short-break'
    }
    return 'focus'
  }, [timerSettings.longBreakInterval])

  const shouldAutoStartNextPhase = useCallback((phase: TimerState['currentPhase']): boolean => {
    if (phase === 'focus') return timerSettings.autoStartBreaks
    return timerSettings.autoStartFocus
  }, [timerSettings])

  // WebSocket event handlers
  const handleSessionStarted = useCallback((data: { userId: string; sessionId: string }) => {
    // Handle session started by other devices/tabs
    if (data.userId === userId && data.sessionId !== currentSession?.id) {
      // Sync session state
      console.log('Session started on another device:', data)
    }
  }, [userId, currentSession?.id])

  const handleSessionEnded = useCallback((data: { userId: string }) => {
    if (data.userId === userId) {
      setCurrentSession(null)
      setTimerState(DEFAULT_TIMER_STATE)
    }
  }, [userId])

  const handlePhaseCompleted = useCallback((data: { userId: string }) => {
    if (data.userId === userId) {
      console.log('Phase completed:', data)
    }
  }, [userId])

  const handleSettingsUpdated = useCallback((data: { userId: string; settings: TimerSettings }) => {
    if (data.userId === userId) {
      setTimerSettings(data.settings)
    }
  }, [userId])

  // Set up WebSocket event listeners
  useEffect(() => {
    if (!isConnected) return

    const unsubscribeFunctions = [
      on('timer:session_started', (data: unknown) => handleSessionStarted(data as { userId: string; sessionId: string })),
      on('timer:session_ended', (data: unknown) => handleSessionEnded(data as { userId: string })),
      on('timer:phase_completed', (data: unknown) => handlePhaseCompleted(data as { userId: string })),
      on('timer:settings_updated', (data: unknown) => handleSettingsUpdated(data as { userId: string; settings: TimerSettings })),
    ]

    return () => {
      unsubscribeFunctions.forEach(unsubscribe => unsubscribe())
    }
  }, [isConnected, on, handleSessionStarted, handleSessionEnded, handlePhaseCompleted, handleSettingsUpdated])

  // Initialize Web Audio API for sound notifications
  useEffect(() => {
    if (timerSettings.soundEnabled && !audioContextRef.current) {
      try {
        audioContextRef.current = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)()
      } catch (error) {
        console.warn('Web Audio API not supported:', error)
      }
    }
  }, [timerSettings.soundEnabled])

  // Timer interval management
  useEffect(() => {
    if (timerState.isRunning && !timerState.isPaused && timerState.timeRemaining > 0) {
      timerIntervalRef.current = setInterval(() => {
        setTimerState(prev => {
          const newTimeRemaining = Math.max(0, prev.timeRemaining - 1)
          
          if (newTimeRemaining === 0) {
            // Phase completed - will be handled by separate effect
            return { ...prev, timeRemaining: 0, isRunning: false }
          }
          
          return { ...prev, timeRemaining: newTimeRemaining }
        })
      }, 1000)
    } else {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current)
        timerIntervalRef.current = null
      }
    }

    return () => {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current)
      }
    }
  }, [timerState.isRunning, timerState.isPaused, timerState.timeRemaining])

  // Handle phase completion when time reaches zero
  useEffect(() => {
    if (!timerState.isRunning && timerState.timeRemaining === 0 && timerState.currentPhase !== 'idle') {
      handlePhaseCompletion(timerState)
    }
  }, [timerState, handlePhaseCompletion])

  const handlePhaseCompletion = useCallback((completedState: TimerState) => {
    playNotificationSound(completedState.currentPhase + 'End')
    showNotification(completedState.currentPhase)
    
    // Emit phase completion to WebSocket
    emit('timer:phase_complete', {
      userId,
      sessionId: currentSession?.id,
      phase: completedState.currentPhase,
      duration: getPhaseLength(completedState.currentPhase),
      timestamp: new Date().toISOString(),
    })

    // Auto-start next phase if enabled
    if (shouldAutoStartNextPhase(completedState.currentPhase)) {
      const nextPhase = getNextPhase(completedState)
      // We'll define startTimer later, so avoid the circular dependency for now
      setTimeout(() => {
        // This will be handled by external timer management
        console.log('Auto-starting next phase:', nextPhase)
      }, 1000)
    } else {
      // Update presence to idle
      updatePresence('online', 'Timer phase completed')
    }
  }, [currentSession, emit, userId, updatePresence, getPhaseLength, shouldAutoStartNextPhase, getNextPhase, playNotificationSound, showNotification])

  const startTimer = useCallback((phase?: TimerState['currentPhase'], hiveId?: string) => {
    const targetPhase = phase || 'focus'
    const duration = getPhaseLength(targetPhase)
    const timeInSeconds = duration * 60
    
    phaseStartTimeRef.current = new Date()
    
    // Start new session if not already started
    if (!currentSession && targetPhase === 'focus') {
      const newSession: SessionStats = {
        id: generateSessionId(),
        userId,
        user: { id: userId, name: '', username: '', email: '', firstName: '', lastName: '', isEmailVerified: false, createdAt: '', updatedAt: '' },
        hiveId: hiveId || currentPresence?.hiveId,
        date: new Date().toISOString().split('T')[0],
        focusTime: 0,
        breakTime: 0,
        completedCycles: 0,
        targetCycles: 4, // Default Pomodoro cycle
        productivity: { rating: 0 },
        distractions: 0,
        goals: [],
        achievements: [],
      }
      
      setCurrentSession(newSession)
      sessionStartTimeRef.current = new Date()
      
      emit('timer:session_start', {
        userId,
        sessionId: newSession.id,
        hiveId: newSession.hiveId,
        timestamp: new Date().toISOString(),
      })
    }

    setTimerState(prev => ({
      ...prev,
      currentPhase: targetPhase,
      timeRemaining: timeInSeconds,
      isRunning: true,
      isPaused: false,
      currentCycle: targetPhase === 'focus' ? prev.currentCycle + 1 : prev.currentCycle,
    }))

    // Update presence based on phase
    const presenceStatus = targetPhase === 'focus' ? 'focusing' : 'break'
    const activity = targetPhase === 'focus' 
      ? `Focus session (${duration}min)` 
      : `${targetPhase.replace('-', ' ')} (${duration}min)`
    
    updatePresence(presenceStatus, activity)
    
    // Play start sound
    playNotificationSound(targetPhase + 'Start')
    
    // Emit timer start to WebSocket
    emit('timer:start', {
      userId,
      sessionId: currentSession?.id,
      phase: targetPhase,
      duration,
      hiveId: hiveId || currentPresence?.hiveId,
      timestamp: new Date().toISOString(),
    })
  }, [
    getPhaseLength, 
    currentSession, 
    userId, 
    currentPresence?.hiveId, 
    emit, 
    updatePresence,
    generateSessionId,
    playNotificationSound
  ])

  const pauseTimer = useCallback(() => {
    setTimerState(prev => ({ ...prev, isPaused: true, isRunning: false }))
    updatePresence('online', 'Timer paused')
    
    emit('timer:pause', {
      userId,
      sessionId: currentSession?.id,
      timestamp: new Date().toISOString(),
    })
  }, [userId, currentSession?.id, emit, updatePresence])

  const resumeTimer = useCallback(() => {
    setTimerState(prev => ({ ...prev, isPaused: false, isRunning: true }))
    
    const presenceStatus = timerState.currentPhase === 'focus' ? 'focusing' : 'break'
    const activity = `Resumed ${timerState.currentPhase.replace('-', ' ')}`
    updatePresence(presenceStatus, activity)
    
    emit('timer:resume', {
      userId,
      sessionId: currentSession?.id,
      timestamp: new Date().toISOString(),
    })
  }, [timerState.currentPhase, userId, currentSession?.id, emit, updatePresence])

  const stopTimer = useCallback(() => {
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current)
      timerIntervalRef.current = null
    }
    
    setTimerState(DEFAULT_TIMER_STATE)
    updatePresence('online', 'Timer stopped')
    
    emit('timer:stop', {
      userId,
      sessionId: currentSession?.id,
      timestamp: new Date().toISOString(),
    })
  }, [userId, currentSession?.id, emit, updatePresence])

  const skipPhase = useCallback(() => {
    if (!timerState.isRunning && !timerState.isPaused) return
    
    // Complete current phase artificially
    handlePhaseCompletion(timerState)
    
    emit('timer:skip_phase', {
      userId,
      sessionId: currentSession?.id,
      phase: timerState.currentPhase,
      timestamp: new Date().toISOString(),
    })
  }, [timerState, handlePhaseCompletion, userId, currentSession?.id, emit])

  const updateSettings = useCallback((settings: Partial<TimerSettings>) => {
    setTimerSettings(prev => ({ ...prev, ...settings }))
    
    emit('timer:settings_update', {
      userId,
      settings: { ...timerSettings, ...settings },
      timestamp: new Date().toISOString(),
    })
  }, [timerSettings, userId, emit])

  const addGoal = useCallback((description: string, priority: SessionGoal['priority']) => {
    if (!currentSession) return
    
    const newGoal: SessionGoal = {
      id: generateGoalId(),
      description,
      priority,
      isCompleted: false,
    }
    
    setCurrentSession(prev => prev ? {
      ...prev,
      goals: [...prev.goals, newGoal]
    } : null)
    
    emit('timer:goal_added', {
      userId,
      sessionId: currentSession.id,
      goal: newGoal,
      timestamp: new Date().toISOString(),
    })
  }, [currentSession, userId, emit, generateGoalId])

  const completeGoal = useCallback((goalId: string) => {
    if (!currentSession) return
    
    setCurrentSession(prev => prev ? {
      ...prev,
      goals: prev.goals.map(goal => 
        goal.id === goalId 
          ? { ...goal, isCompleted: true, completedAt: new Date().toISOString() }
          : goal
      )
    } : null)
    
    emit('timer:goal_completed', {
      userId,
      sessionId: currentSession.id,
      goalId,
      timestamp: new Date().toISOString(),
    })
  }, [currentSession, userId, emit])

  const removeGoal = useCallback((goalId: string) => {
    if (!currentSession) return
    
    setCurrentSession(prev => prev ? {
      ...prev,
      goals: prev.goals.filter(goal => goal.id !== goalId)
    } : null)
    
    emit('timer:goal_removed', {
      userId,
      sessionId: currentSession.id,
      goalId,
      timestamp: new Date().toISOString(),
    })
  }, [currentSession, userId, emit])

  const recordDistraction = useCallback(() => {
    if (!currentSession) return
    
    setCurrentSession(prev => prev ? {
      ...prev,
      distractions: prev.distractions + 1
    } : null)
    
    emit('timer:distraction_recorded', {
      userId,
      sessionId: currentSession.id,
      timestamp: new Date().toISOString(),
    })
  }, [currentSession, userId, emit])

  const endSession = useCallback((productivity: SessionStats['productivity']) => {
    if (!currentSession) return
    
    const sessionEndTime = new Date()
    const sessionDuration = sessionStartTimeRef.current 
      ? (sessionEndTime.getTime() - sessionStartTimeRef.current.getTime()) / 1000 / 60
      : 0
    
    const updatedSession: SessionStats = {
      ...currentSession,
      productivity,
      focusTime: sessionDuration, // This should be calculated more precisely
    }
    
    emit('timer:session_end', {
      userId,
      sessionId: currentSession.id,
      session: updatedSession,
      timestamp: sessionEndTime.toISOString(),
    })
    
    setCurrentSession(null)
    setTimerState(DEFAULT_TIMER_STATE)
    updatePresence('online', 'Session completed')
    
    sessionStartTimeRef.current = null
  }, [currentSession, userId, emit, updatePresence])


  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current)
      }
      if (audioContextRef.current) {
        audioContextRef.current.close()
      }
    }
  }, [])

  const value: TimerContextType = {
    timerState,
    timerSettings,
    currentSession,
    startTimer,
    pauseTimer,
    resumeTimer,
    stopTimer,
    skipPhase,
    updateSettings,
    addGoal,
    completeGoal,
    removeGoal,
    recordDistraction,
    endSession,
  }

  return (
    <TimerContext.Provider value={value}>
      {children}
    </TimerContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useTimer = (): TimerContextType => {
  const context = useContext(TimerContext)
  if (context === undefined) {
    throw new Error('useTimer must be used within a TimerProvider')
  }
  return context
}

export default TimerContext