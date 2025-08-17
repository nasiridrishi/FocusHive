import React, { 
  createContext, 
  useContext, 
  useEffect, 
  useRef, 
  useState, 
  useCallback, 
  useMemo 
} from 'react'
import { io, Socket } from 'socket.io-client'

// eslint-disable-next-line react-refresh/only-export-components
export enum ConnectionState {
  DISCONNECTED = 'disconnected',
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  RECONNECTING = 'reconnecting',
  ERROR = 'error',
}

interface WebSocketContextType {
  socket: Socket | null
  connectionState: ConnectionState
  isConnected: boolean
  reconnectCount: number
  lastError: string | null
  connect: () => void
  disconnect: () => void
  emit: (event: string, data?: unknown) => void
  on: (event: string, handler: (...args: unknown[]) => void) => () => void
  off: (event: string, handler?: (...args: unknown[]) => void) => void
  // Performance optimization methods
  batchEmit: (events: Array<{ event: string; data?: unknown }>) => void
  throttledEmit: (event: string, data?: unknown, delay?: number) => void
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined)

interface WebSocketProviderProps {
  children: React.ReactNode
  url?: string
  autoConnect?: boolean
  options?: {
    reconnectionAttempts?: number
    reconnectionDelay?: number
    timeout?: number
    heartbeatInterval?: number
    messageQueueSize?: number
  }
}

const DEFAULT_OPTIONS = {
  reconnectionAttempts: 10,
  reconnectionDelay: 1000,
  timeout: 20000,
  heartbeatInterval: 30000,
  messageQueueSize: 100,
}

export const OptimizedWebSocketProvider: React.FC<WebSocketProviderProps> = ({
  children,
  url = 'http://localhost:8080',
  autoConnect = true,
  options = DEFAULT_OPTIONS,
}) => {
  const [connectionState, setConnectionState] = useState<ConnectionState>(ConnectionState.DISCONNECTED)
  const [reconnectCount, setReconnectCount] = useState(0)
  const [lastError, setLastError] = useState<string | null>(null)
  
  const socketRef = useRef<Socket | null>(null)
  const reconnectTimeoutRef = useRef<number | null>(null)
  const isManualDisconnect = useRef(false)
  const messageQueueRef = useRef<Array<{ event: string; data?: unknown }>>([])
  const heartbeatIntervalRef = useRef<number | null>(null)
  const throttleTimersRef = useRef<Map<string, number>>(new Map())
  const performanceMetricsRef = useRef({
    messagesReceived: 0,
    messagesSent: 0,
    connectionTime: 0,
    lastHeartbeat: 0,
  })

  // Memoize options to prevent unnecessary reconnections
  const mergedOptions = useMemo(() => ({
    ...DEFAULT_OPTIONS,
    ...options,
  }), [options])

  const clearReconnectTimeout = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
  }, [])

  const clearHeartbeat = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current)
      heartbeatIntervalRef.current = null
    }
  }, [])

  const startHeartbeat = useCallback(() => {
    clearHeartbeat()
    heartbeatIntervalRef.current = window.setInterval(() => {
      if (socketRef.current?.connected) {
        socketRef.current.emit('heartbeat', { timestamp: Date.now() })
        performanceMetricsRef.current.lastHeartbeat = Date.now()
      }
    }, mergedOptions.heartbeatInterval)
  }, [mergedOptions.heartbeatInterval, clearHeartbeat])

  const processMessageQueue = useCallback(() => {
    if (socketRef.current?.connected && messageQueueRef.current.length > 0) {
      const messages = messageQueueRef.current.splice(0, mergedOptions.messageQueueSize)
      messages.forEach(({ event, data }) => {
        socketRef.current?.emit(event, data)
        performanceMetricsRef.current.messagesSent++
      })
    }
  }, [mergedOptions.messageQueueSize])

  const connect = useCallback((): void => {
    if (socketRef.current?.connected) {
      return
    }

    setConnectionState(ConnectionState.CONNECTING)
    setLastError(null)
    isManualDisconnect.current = false
    performanceMetricsRef.current.connectionTime = Date.now()

    const socket = io(url, {
      timeout: mergedOptions.timeout,
      reconnection: false, // Manual reconnection for better control
      autoConnect: false,
      // Performance optimizations
      forceNew: false,
      multiplex: true,
      upgrade: true,
      rememberUpgrade: true,
    })

    socket.on('connect', () => {
      setConnectionState(ConnectionState.CONNECTED)
      setReconnectCount(0)
      setLastError(null)
      clearReconnectTimeout()
      startHeartbeat()
      processMessageQueue()
    })

    socket.on('disconnect', (reason) => {
      setConnectionState(ConnectionState.DISCONNECTED)
      clearHeartbeat()
      
      if (!isManualDisconnect.current && reason !== 'io client disconnect') {
        scheduleReconnectRef.current?.()
      }
    })

    socket.on('connect_error', (error) => {
      setConnectionState(ConnectionState.ERROR)
      setLastError(error.message)
      clearHeartbeat()
      
      if (!isManualDisconnect.current) {
        scheduleReconnectRef.current?.()
      }
    })

    // Performance monitoring
    socket.on('pong', () => {
      performanceMetricsRef.current.messagesReceived++
    })

    socketRef.current = socket
    socket.connect()
  }, [url, mergedOptions.timeout, clearReconnectTimeout, startHeartbeat, processMessageQueue])

  const scheduleReconnectRef = useRef<() => void>()
  
  const scheduleReconnect = useCallback(() => {
    if (isManualDisconnect.current || reconnectCount >= mergedOptions.reconnectionAttempts) {
      return
    }

    clearReconnectTimeout()
    
    // Exponential backoff with jitter and max cap
    const baseDelay = mergedOptions.reconnectionDelay
    const exponentialDelay = Math.min(baseDelay * Math.pow(2, reconnectCount), 30000)
    const jitter = Math.random() * 1000
    const delay = exponentialDelay + jitter

    setConnectionState(ConnectionState.RECONNECTING)
    
    reconnectTimeoutRef.current = window.setTimeout(() => {
      setReconnectCount(prev => prev + 1)
      connect()
    }, delay)
  }, [reconnectCount, mergedOptions.reconnectionDelay, mergedOptions.reconnectionAttempts, clearReconnectTimeout, connect])
  
  scheduleReconnectRef.current = scheduleReconnect

  const disconnect = useCallback(() => {
    isManualDisconnect.current = true
    clearReconnectTimeout()
    clearHeartbeat()
    
    // Clear throttle timers
    throttleTimersRef.current.forEach(timerId => clearTimeout(timerId))
    throttleTimersRef.current.clear()
    
    if (socketRef.current) {
      socketRef.current.disconnect()
      socketRef.current = null
    }
    
    setConnectionState(ConnectionState.DISCONNECTED)
    setReconnectCount(0)
    setLastError(null)
    messageQueueRef.current = []
  }, [clearReconnectTimeout, clearHeartbeat])

  const emit = useCallback((event: string, data?: unknown) => {
    if (socketRef.current?.connected) {
      socketRef.current.emit(event, data)
      performanceMetricsRef.current.messagesSent++
    } else {
      // Queue message for when connection is restored
      if (messageQueueRef.current.length < mergedOptions.messageQueueSize) {
        messageQueueRef.current.push({ event, data })
      }
    }
  }, [mergedOptions.messageQueueSize])

  // Performance optimization: Batch multiple emissions
  const batchEmit = useCallback((events: Array<{ event: string; data?: unknown }>) => {
    if (socketRef.current?.connected) {
      events.forEach(({ event, data }) => {
        socketRef.current?.emit(event, data)
        performanceMetricsRef.current.messagesSent++
      })
    } else {
      // Queue all events
      events.forEach(({ event, data }) => {
        if (messageQueueRef.current.length < mergedOptions.messageQueueSize) {
          messageQueueRef.current.push({ event, data })
        }
      })
    }
  }, [mergedOptions.messageQueueSize])

  // Performance optimization: Throttled emissions
  const throttledEmit = useCallback((event: string, data?: unknown, delay: number = 1000) => {
    const timerId = throttleTimersRef.current.get(event)
    
    if (timerId) {
      clearTimeout(timerId)
    }
    
    const newTimerId = window.setTimeout(() => {
      emit(event, data)
      throttleTimersRef.current.delete(event)
    }, delay)
    
    throttleTimersRef.current.set(event, newTimerId)
  }, [emit])

  const on = useCallback((event: string, handler: (...args: unknown[]) => void) => {
    if (socketRef.current) {
      socketRef.current.on(event, (...args: unknown[]) => {
        performanceMetricsRef.current.messagesReceived++
        handler(...args)
      })
    }
    
    // Return cleanup function
    return () => {
      if (socketRef.current) {
        socketRef.current.off(event, handler)
      }
    }
  }, [])

  const off = useCallback((event: string, handler?: (...args: unknown[]) => void) => {
    if (socketRef.current) {
      if (handler) {
        socketRef.current.off(event, handler)
      } else {
        socketRef.current.off(event)
      }
    }
  }, [])

  // Auto-connect on mount
  useEffect(() => {
    if (autoConnect) {
      connect()
    }

    return () => {
      disconnect()
    }
  }, [autoConnect, connect, disconnect])

  // Performance monitoring and cleanup
  useEffect(() => {
    const interval = setInterval(() => {
      const metrics = performanceMetricsRef.current
      if (process.env.NODE_ENV === 'development') {
        console.debug('WebSocket Performance Metrics:', {
          sent: metrics.messagesSent,
          received: metrics.messagesReceived,
          uptime: Date.now() - metrics.connectionTime,
          queueSize: messageQueueRef.current.length,
        })
      }
    }, 30000) // Log every 30 seconds in development

    return () => clearInterval(interval)
  }, [])

  // Handle visibility change for optimized reconnection
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible' && 
          connectionState === ConnectionState.DISCONNECTED && 
          !isManualDisconnect.current) {
        // Small delay to allow network to stabilize
        setTimeout(connect, 1000)
      } else if (document.visibilityState === 'hidden') {
        // Optionally reduce heartbeat frequency when hidden
        clearHeartbeat()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [connectionState, connect, clearHeartbeat])

  // Memoize context value to prevent unnecessary re-renders
  const value: WebSocketContextType = useMemo(() => ({
    socket: socketRef.current,
    connectionState,
    isConnected: connectionState === ConnectionState.CONNECTED,
    reconnectCount,
    lastError,
    connect,
    disconnect,
    emit,
    on,
    off,
    batchEmit,
    throttledEmit,
  }), [
    connectionState,
    reconnectCount,
    lastError,
    connect,
    disconnect,
    emit,
    on,
    off,
    batchEmit,
    throttledEmit,
  ])

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useOptimizedWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext)
  if (context === undefined) {
    throw new Error('useOptimizedWebSocket must be used within an OptimizedWebSocketProvider')
  }
  return context
}

export default WebSocketContext