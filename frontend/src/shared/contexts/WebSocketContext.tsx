import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react'
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
  
  const socketRef = useRef<Socket | null>(null)
  const reconnectTimeoutRef = useRef<number | null>(null)
  const isManualDisconnect = useRef(false)

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

    const socket = io(url, {
      timeout: options.timeout || DEFAULT_OPTIONS.timeout,
      reconnection: false, // We handle reconnection manually
      autoConnect: false,
    })

    socket.on('connect', () => {
      setConnectionState(ConnectionState.CONNECTED)
      setReconnectCount(0)
      setLastError(null)
      clearReconnectTimeout()
    })

    socket.on('disconnect', (reason) => {
      setConnectionState(ConnectionState.DISCONNECTED)
      
      if (!isManualDisconnect.current && reason !== 'io client disconnect') {
        if (scheduleReconnectRef.current) {
          scheduleReconnectRef.current()
        }
      }
    })

    socket.on('connect_error', (error) => {
      setConnectionState(ConnectionState.ERROR)
      setLastError(error.message)
      
      if (!isManualDisconnect.current) {
        if (scheduleReconnectRef.current) {
          scheduleReconnectRef.current()
        }
      }
    })

    socketRef.current = socket
    socket.connect()
  }, [url, options.timeout, clearReconnectTimeout])

  const scheduleReconnectRef = useRef<() => void>()
  
  const scheduleReconnect = useCallback(() => {
    if (isManualDisconnect.current || reconnectCount >= (options.reconnectionAttempts || DEFAULT_OPTIONS.reconnectionAttempts)) {
      return
    }

    clearReconnectTimeout()
    
    // Exponential backoff with jitter
    const baseDelay = options.reconnectionDelay || DEFAULT_OPTIONS.reconnectionDelay
    const exponentialDelay = Math.min(baseDelay * Math.pow(2, reconnectCount), 30000)
    const jitter = Math.random() * 1000
    const delay = exponentialDelay + jitter

    setConnectionState(ConnectionState.RECONNECTING)
    
    reconnectTimeoutRef.current = window.setTimeout(() => {
      setReconnectCount(prev => prev + 1)
      connect()
    }, delay)
  }, [reconnectCount, options.reconnectionDelay, options.reconnectionAttempts, clearReconnectTimeout])
  
  scheduleReconnectRef.current = scheduleReconnect

  const disconnect = useCallback(() => {
    isManualDisconnect.current = true
    clearReconnectTimeout()
    
    if (socketRef.current) {
      socketRef.current.disconnect()
      socketRef.current = null
    }
    
    setConnectionState(ConnectionState.DISCONNECTED)
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

  // Auto-connect on mount
  useEffect(() => {
    if (autoConnect) {
      connect()
    }

    return () => {
      disconnect()
    }
  }, [autoConnect, connect, disconnect])

  // Handle visibility change for reconnection
  useEffect(() => {
    const handleVisibilityChange = () => {
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