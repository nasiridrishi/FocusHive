import { useEffect, useRef, useCallback, useState } from 'react'
import { io, Socket } from 'socket.io-client'
import { WebSocketMessage, UseMusicWebSocketOptions } from '../types'

interface WebSocketState {
  isConnected: boolean
  isConnecting: boolean
  error: string | null
  lastMessage: WebSocketMessage | null
  reconnectAttempts: number
}

/**
 * Custom hook for music WebSocket connection
 * Handles real-time updates for collaborative features
 */
export const useMusicWebSocket = (options: UseMusicWebSocketOptions = {}) => {
  const [state, setState] = useState<WebSocketState>({
    isConnected: false,
    isConnecting: false,
    error: null,
    lastMessage: null,
    reconnectAttempts: 0,
  })

  const socketRef = useRef<Socket | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const messageQueueRef = useRef<any[]>([])
  const { hiveId, onMessage, onConnect, onDisconnect } = options

  // Connect to WebSocket
  const connect = useCallback(() => {
    if (socketRef.current?.connected) {
      return
    }

    setState(prev => ({ ...prev, isConnecting: true, error: null }))

    try {
      socketRef.current = io('ws://localhost:8084', {
        path: '/ws/music',
        transports: ['websocket'],
        query: hiveId ? { hiveId } : {},
        reconnection: true,
        reconnectionAttempts: 5,
        reconnectionDelay: 1000,
        reconnectionDelayMax: 5000,
        maxReconnectionAttempts: 5,
        timeout: 20000,
      })

      const socket = socketRef.current

      // Connection events
      socket.on('connect', () => {
        setState(prev => ({ 
          ...prev, 
          isConnected: true, 
          isConnecting: false, 
          error: null,
          reconnectAttempts: 0
        }))
        
        // Send queued messages
        messageQueueRef.current.forEach(message => {
          socket.emit(message.type, message.data)
        })
        messageQueueRef.current = []
        
        onConnect?.()
      })

      socket.on('disconnect', (reason) => {
        setState(prev => ({ 
          ...prev, 
          isConnected: false, 
          isConnecting: false,
          error: `Disconnected: ${reason}`
        }))
        onDisconnect?.()
      })

      socket.on('connect_error', (error) => {
        setState(prev => ({ 
          ...prev, 
          isConnected: false, 
          isConnecting: false,
          error: `Connection error: ${error.message}`,
          reconnectAttempts: prev.reconnectAttempts + 1
        }))
      })

      socket.on('reconnect', (attemptNumber) => {
        setState(prev => ({ 
          ...prev, 
          isConnected: true, 
          isConnecting: false,
          error: null,
          reconnectAttempts: 0
        }))
      })

      socket.on('reconnect_error', (error) => {
        setState(prev => ({ 
          ...prev,
          error: `Reconnection failed: ${error.message}`,
          reconnectAttempts: prev.reconnectAttempts + 1
        }))
      })

      socket.on('reconnect_failed', () => {
        setState(prev => ({ 
          ...prev,
          error: 'Failed to reconnect after maximum attempts',
          isConnecting: false
        }))
      })

      // Music-specific events
      socket.on('track_added', (data) => {
        const message: WebSocketMessage = {
          type: 'track_added',
          payload: data,
          timestamp: new Date().toISOString(),
          userId: data.userId || 'unknown'
        }
        setState(prev => ({ ...prev, lastMessage: message }))
        onMessage?.(message)
      })

      socket.on('track_voted', (data) => {
        const message: WebSocketMessage = {
          type: 'track_voted',
          payload: data,
          timestamp: new Date().toISOString(),
          userId: data.userId || 'unknown'
        }
        setState(prev => ({ ...prev, lastMessage: message }))
        onMessage?.(message)
      })

      socket.on('queue_updated', (data) => {
        const message: WebSocketMessage = {
          type: 'queue_updated',
          payload: data,
          timestamp: new Date().toISOString(),
          userId: 'system'
        }
        setState(prev => ({ ...prev, lastMessage: message }))
        onMessage?.(message)
      })

      socket.on('track_changed', (data) => {
        const message: WebSocketMessage = {
          type: 'track_changed',
          payload: data,
          timestamp: new Date().toISOString(),
          userId: data.userId || 'system'
        }
        setState(prev => ({ ...prev, lastMessage: message }))
        onMessage?.(message)
      })

      socket.on('user_joined', (data) => {
        const message: WebSocketMessage = {
          type: 'user_joined',
          payload: data,
          timestamp: new Date().toISOString(),
          userId: data.userId || 'unknown'
        }
        setState(prev => ({ ...prev, lastMessage: message }))
        onMessage?.(message)
      })

      socket.on('user_left', (data) => {
        const message: WebSocketMessage = {
          type: 'user_left',
          payload: data,
          timestamp: new Date().toISOString(),
          userId: data.userId || 'unknown'
        }
        setState(prev => ({ ...prev, lastMessage: message }))
        onMessage?.(message)
      })

    } catch (error) {
      setState(prev => ({ 
        ...prev, 
        isConnecting: false,
        error: `Connection failed: ${error instanceof Error ? error.message : 'Unknown error'}`
      }))
    }
  }, [hiveId, onConnect, onDisconnect, onMessage])

  // Disconnect from WebSocket
  const disconnect = useCallback(() => {
    if (socketRef.current) {
      socketRef.current.disconnect()
      socketRef.current = null
    }
    
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }

    setState(prev => ({ 
      ...prev, 
      isConnected: false, 
      isConnecting: false,
      error: null
    }))
  }, [])

  // Send message with queueing support
  const sendMessage = useCallback((type: string, data: any) => {
    if (socketRef.current?.connected) {
      socketRef.current.emit(type, data)
    } else {
      // Queue message for when connection is restored
      messageQueueRef.current.push({ type, data })
    }
  }, [])

  // Join a hive
  const joinHive = useCallback((newHiveId: string) => {
    sendMessage('join_hive', { hiveId: newHiveId })
  }, [sendMessage])

  // Leave a hive
  const leaveHive = useCallback((hiveIdToLeave: string) => {
    sendMessage('leave_hive', { hiveId: hiveIdToLeave })
  }, [sendMessage])

  // Add track to queue
  const addTrackToQueue = useCallback((trackId: string, position?: number) => {
    sendMessage('add_to_queue', { trackId, position })
  }, [sendMessage])

  // Vote on track
  const voteOnTrack = useCallback((queueId: string, vote: 'up' | 'down') => {
    sendMessage('vote_track', { queueId, vote })
  }, [sendMessage])

  // Update playback state
  const updatePlaybackState = useCallback((state: any) => {
    sendMessage('playback_update', state)
  }, [sendMessage])

  // Manual reconnect
  const reconnect = useCallback(() => {
    if (state.isConnecting || state.isConnected) {
      return
    }

    disconnect()
    
    // Add a small delay before reconnecting
    reconnectTimeoutRef.current = setTimeout(() => {
      connect()
    }, 1000)
  }, [state.isConnecting, state.isConnected, disconnect, connect])

  // Auto-connect on mount and hiveId change
  useEffect(() => {
    connect()

    return () => {
      disconnect()
    }
  }, [hiveId]) // Reconnect when hiveId changes

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect()
    }
  }, [disconnect])

  // Connection health check
  const checkConnection = useCallback(() => {
    if (socketRef.current) {
      return socketRef.current.connected
    }
    return false
  }, [])

  // Get connection info
  const getConnectionInfo = useCallback(() => {
    if (socketRef.current) {
      return {
        id: socketRef.current.id,
        connected: socketRef.current.connected,
        transport: socketRef.current.io.engine.transport.name,
      }
    }
    return null
  }, [])

  return {
    // Connection state
    isConnected: state.isConnected,
    isConnecting: state.isConnecting,
    error: state.error,
    lastMessage: state.lastMessage,
    reconnectAttempts: state.reconnectAttempts,
    
    // Connection controls
    connect,
    disconnect,
    reconnect,
    
    // Message handling
    sendMessage,
    
    // Music-specific actions
    joinHive,
    leaveHive,
    addTrackToQueue,
    voteOnTrack,
    updatePlaybackState,
    
    // Utilities
    checkConnection,
    getConnectionInfo,
    
    // Computed values
    canReconnect: !state.isConnecting && !state.isConnected && state.reconnectAttempts < 5,
    isHealthy: state.isConnected && !state.error,
  }
}