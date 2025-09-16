import React, {createContext, useCallback, useContext, useEffect, useLayoutEffect, useRef, useState} from 'react'
import {io, Socket} from 'socket.io-client'

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
  }
}

const DEFAULT_OPTIONS = {
  reconnectionAttempts: 10,
  reconnectionDelay: 1000,
  timeout: 20000,
}

export const WebSocketProvider: React.FC<WebSocketProviderProps> = ({
                                                                      children,
                                                                      url = 'http://localhost:8080',
                                                                      autoConnect = true,
                                                                      options = DEFAULT_OPTIONS,
                                                                    }) => {
  const [connectionState, setConnectionState] = useState<ConnectionState>(ConnectionState.DISCONNECTED)
  const [reconnectCount, setReconnectCount] = useState(0)
  const [lastError, setLastError] = useState<string | null>(null)

  // Use refs for values that need to be accessed in callbacks
  const reconnectCountRef = useRef(0)

  const socketRef = useRef<Socket | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const isManualDisconnect = useRef(false)
  const isSocketInitialized = useRef(false)

  const clearReconnectTimeout = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
  }, [])

  const connect = useCallback((): void => {
    if (socketRef.current?.connected) {
      return
    }

    setConnectionState(ConnectionState.CONNECTING)
    setLastError(null)
    isManualDisconnect.current = false

    if (socketRef.current) {
      socketRef.current.connect()
    }
  }, [])

  // Note: scheduleReconnect is defined inside the socket initialization useEffect
  // to ensure stable references and avoid circular dependencies

  const disconnect = useCallback(() => {
    isManualDisconnect.current = true
    clearReconnectTimeout()

    if (socketRef.current) {
      socketRef.current.disconnect()
      // Don't null the socket reference - we can reuse it for reconnection
    }

    setConnectionState(ConnectionState.DISCONNECTED)
    reconnectCountRef.current = 0
    setReconnectCount(0)
    setLastError(null)
  }, [clearReconnectTimeout])

  const emit = useCallback((event: string, data?: unknown) => {
    if (socketRef.current?.connected) {
      socketRef.current.emit(event, data)
    }
  }, [])

  const on = useCallback((event: string, handler: (...args: unknown[]) => void) => {
    if (socketRef.current) {
      socketRef.current.on(event, handler)
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

  // Initialize socket on mount and auto-connect if needed
  useEffect(() => {
    // Define clearReconnectTimeout inside useEffect
    const clearReconnectTimeoutInternal = () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current)
        reconnectTimeoutRef.current = null
      }
    }

    // Define scheduleReconnect inside useEffect for stable reference
    const scheduleReconnectInternal = () => {
      if (isManualDisconnect.current || reconnectCountRef.current >= (options.reconnectionAttempts || DEFAULT_OPTIONS.reconnectionAttempts)) {
        return
      }

      clearReconnectTimeoutInternal()

      // Exponential backoff with jitter
      const baseDelay = options.reconnectionDelay || DEFAULT_OPTIONS.reconnectionDelay
      const exponentialDelay = Math.min(baseDelay * Math.pow(2, reconnectCountRef.current), 30000)
      const jitter = Math.random() * 1000
      const delay = exponentialDelay + jitter

      reconnectTimeoutRef.current = setTimeout(() => {
        setConnectionState(ConnectionState.RECONNECTING)
        reconnectCountRef.current += 1
        setReconnectCount(prev => prev + 1)
        // Direct socket connection
        if (socketRef.current && !socketRef.current.connected) {
          socketRef.current.connect()
        }
      }, delay)
    }

    // Initialize socket immediately on mount if not already initialized
    if (!isSocketInitialized.current && !socketRef.current) {
      const socket = io(url, {
        timeout: options.timeout || DEFAULT_OPTIONS.timeout,
        reconnection: false, // We handle reconnection manually
        autoConnect: false,
      })

      socket.on('connect', () => {
        setConnectionState(ConnectionState.CONNECTED)
        reconnectCountRef.current = 0
        setReconnectCount(0)
        setLastError(null)
        clearReconnectTimeoutInternal()
      })

      socket.on('disconnect', (reason) => {
        setConnectionState(ConnectionState.DISCONNECTED)

        if (!isManualDisconnect.current && reason !== 'io client disconnect') {
          scheduleReconnectInternal()
        }
      })

      socket.on('connect_error', (error) => {
        setConnectionState(ConnectionState.ERROR)
        setLastError(error.message)

        if (!isManualDisconnect.current) {
          scheduleReconnectInternal()
        }
      })

      socketRef.current = socket
      isSocketInitialized.current = true
    }

    if (autoConnect) {
      connect()
    }

    return () => {
      // Don't disconnect on unmount, just cleanup
      clearReconnectTimeoutInternal()
      if (socketRef.current) {
        // Remove all listeners (removeAllListeners may not exist in mock)
        if (typeof socketRef.current.removeAllListeners === 'function') {
          socketRef.current.removeAllListeners()
        } else if (typeof socketRef.current.off === 'function') {
          // Fallback for mocked sockets
          socketRef.current.off('connect')
          socketRef.current.off('disconnect')
          socketRef.current.off('connect_error')
        }
        socketRef.current.disconnect()
        socketRef.current = null
        isSocketInitialized.current = false
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Handle visibility change for reconnection
  useEffect(() => {
    const handleVisibilityChange = (): void => {
      if (document.visibilityState === 'visible' &&
          connectionState === ConnectionState.DISCONNECTED &&
          !isManualDisconnect.current) {
        connect()
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [connectionState, connect])

  const value: WebSocketContextType = {
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
  }

  return (
      <WebSocketContext.Provider value={value}>
        {children}
      </WebSocketContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext)
  if (context === undefined) {
    throw new Error('useWebSocket must be used within a WebSocketProvider')
  }
  return context
}

export default WebSocketContext