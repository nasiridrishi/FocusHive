import React, { createContext, useContext, useEffect, useState, useRef } from 'react';
import { io, Socket } from 'socket.io-client';
import { useAuth } from './AuthContext';
import type { Room, ParticipantStatus, User } from '@focushive/shared';

interface SocketContextType {
  socket: Socket | null;
  connected: boolean;
  authenticated: boolean;
  currentRoom: Room | null;
  participants: ParticipantStatus[];
  joinRoom: (roomId: string) => Promise<void>;
  leaveRoom: () => Promise<void>;
  updatePresence: (status: ParticipantStatus['status'], currentTask?: string) => void;
}

const SocketContext = createContext<SocketContextType | undefined>(undefined);

export const SocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const [socket, setSocket] = useState<Socket | null>(null);
  const [connected, setConnected] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [currentRoom, setCurrentRoom] = useState<Room | null>(null);
  const [participants, setParticipants] = useState<ParticipantStatus[]>([]);
  const socketRef = useRef<Socket | null>(null);

  useEffect(() => {
    if (!user) {
      // Disconnect if user logs out
      if (socketRef.current) {
        socketRef.current.disconnect();
        socketRef.current = null;
        setSocket(null);
        setConnected(false);
        setAuthenticated(false);
        setCurrentRoom(null);
        setParticipants([]);
      }
      return;
    }

    // Connect with auth token
    const token = localStorage.getItem('token');
    if (!token) return;

    const newSocket = io(import.meta.env.VITE_API_URL || 'http://localhost:3000', {
      auth: { token },
      transports: ['websocket'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: 5
    });

    socketRef.current = newSocket;
    setSocket(newSocket);

    // Connection events
    newSocket.on('connect', () => {
      setConnected(true);
    });

    newSocket.on('disconnect', () => {
      setConnected(false);
    });

    // Auth events
    newSocket.on('auth:success', (data) => {
      setAuthenticated(true);
    });

    newSocket.on('auth:error', (error) => {
      setAuthenticated(false);
      // Handle auth error - maybe redirect to login
    });

    // Room events
    newSocket.on('room:joined', (data: { room: Room; participants: ParticipantStatus[] }) => {
      console.log('[SocketContext] Room joined event:', data);
      console.log('[SocketContext] Participants received:', data.participants?.length || 0);
      setCurrentRoom(data.room);
      setParticipants(data.participants || []);
    });

    newSocket.on('room:left', () => {
      setCurrentRoom(null);
      setParticipants([]);
    });

    newSocket.on('room:error', (error) => {
      // Show error to user
    });

    // Participant events
    newSocket.on('room:user-joined', (data: { user: Partial<User>; participant: ParticipantStatus }) => {
      setParticipants(prev => [...prev, data.participant]);
    });

    newSocket.on('room:user-left', (data: { userId: string }) => {
      setParticipants(prev => prev.filter(p => p.userId !== data.userId));
    });

    newSocket.on('room:participants', (data: { participants: ParticipantStatus[] }) => {
      console.log('[SocketContext] Participants update:', data.participants?.length || 0);
      setParticipants(data.participants || []);
    });

    // Presence events
    newSocket.on('presence:updated', (data: { userId: string; status: ParticipantStatus }) => {
      setParticipants(prev => 
        prev.map(p => {
          if (p.userId === data.userId) {
            // If we receive a full ParticipantStatus object, use it
            if (data.status && typeof data.status === 'object' && 'userId' in data.status) {
              return data.status;
            }
            // Otherwise, just update the status field
            return { ...p, status: data.status.status || data.status };
          }
          return p;
        })
      );
    });

    // Cleanup
    return () => {
      newSocket.disconnect();
      socketRef.current = null;
      setSocket(null);
      setConnected(false);
      setCurrentRoom(null);
      setParticipants([]);
    };
  }, [user]);

  const joinRoom = async (roomId: string): Promise<void> => {
    if (!socket || !connected) {
      throw new Error('Socket not connected');
    }

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Join room timeout'));
      }, 5000);

      socket.once('room:joined', () => {
        clearTimeout(timeout);
        resolve();
      });

      socket.once('room:error', (error) => {
        clearTimeout(timeout);
        reject(new Error(error.message));
      });

      socket.emit('room:join', { roomId });
    });
  };

  const leaveRoom = async (): Promise<void> => {
    if (!socket || !connected || !currentRoom) {
      return;
    }

    return new Promise((resolve) => {
      socket.once('room:left', () => {
        resolve();
      });

      socket.emit('room:leave', { roomId: currentRoom.id });
    });
  };

  const updatePresence = (status: ParticipantStatus['status'], currentTask?: string) => {
    console.log('[SocketContext] updatePresence called:', { status, currentTask, connected, currentRoom: currentRoom?.id });
    
    if (!socket || !connected || !currentRoom) {
      console.log('[SocketContext] Cannot update presence - missing requirements:', { socket: !!socket, connected, currentRoom: !!currentRoom });
      return;
    }

    console.log('[SocketContext] Emitting presence:update event');
    socket.emit('presence:update', { status, currentTask });
  };

  return (
    <SocketContext.Provider value={{
      socket,
      connected,
      authenticated,
      currentRoom,
      participants,
      joinRoom,
      leaveRoom,
      updatePresence
    }}>
      {children}
    </SocketContext.Provider>
  );
};

export const useSocket = () => {
  const context = useContext(SocketContext);
  if (!context) {
    throw new Error('useSocket must be used within SocketProvider');
  }
  return context;
};