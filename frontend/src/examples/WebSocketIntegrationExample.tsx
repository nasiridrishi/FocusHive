import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  List,
  ListItem,
  ListItemText,
  Divider,
  Alert,
} from '@mui/material';
import { useWebSocketWithAuth } from '../hooks/useWebSocketWithAuth';
import { WebSocketConnectionStatus } from '../shared/ui';
import type { WebSocketMessage, NotificationMessage, PresenceUpdate } from '../services/websocket/WebSocketService';

interface WebSocketIntegrationExampleProps {
  authToken?: string | null;
}

/**
 * Example component demonstrating WebSocket integration with authentication
 * This shows how to integrate WebSocket functionality with an auth context
 */
export const WebSocketIntegrationExample: React.FC<WebSocketIntegrationExampleProps> = ({
  authToken
}) => {
  const [messages, setMessages] = useState<WebSocketMessage[]>([]);
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const [presenceUpdates, setPresenceUpdates] = useState<PresenceUpdate[]>([]);

  // Initialize WebSocket with auth integration
  const webSocket = useWebSocketWithAuth({
    token: authToken,
    autoConnect: true,
    onConnect: () => {
      console.log('WebSocket connected!');
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        type: 'INFO',
        event: 'connection',
        payload: 'Connected to server',
        timestamp: new Date().toISOString()
      }]);
    },
    onDisconnect: () => {
      console.log('WebSocket disconnected!');
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        type: 'WARNING',
        event: 'disconnection', 
        payload: 'Disconnected from server',
        timestamp: new Date().toISOString()
      }]);
    },
    onMessage: (message: WebSocketMessage) => {
      console.log('Received message:', message);
      setMessages(prev => [...prev, message].slice(-10)); // Keep last 10 messages
    },
    onNotification: (notification: NotificationMessage) => {
      console.log('Received notification:', notification);
      setNotifications(prev => [...prev, notification].slice(-5)); // Keep last 5 notifications
    },
    onPresenceUpdate: (presence: PresenceUpdate) => {
      console.log('Presence update:', presence);
      setPresenceUpdates(prev => {
        // Update or add presence for this user
        const updated = prev.filter(p => p.userId !== presence.userId);
        return [...updated, presence].slice(-10); // Keep last 10 presence updates
      });
    }
  });

  const sendTestMessage = () => {
    if (webSocket.isConnected) {
      webSocket.sendMessage('/app/test/message', {
        content: 'Hello from frontend!',
        timestamp: new Date().toISOString()
      });
    }
  };

  const updatePresence = () => {
    if (webSocket.isConnected) {
      webSocket.updatePresence('ONLINE');
    }
  };

  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        WebSocket Integration Example
      </Typography>

      {/* Connection Status */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Connection Status
          </Typography>
          <WebSocketConnectionStatus
            connectionInfo={webSocket.connectionInfo}
            onRetry={webSocket.retryConnection}
            showDetails={true}
            variant="detailed"
          />
          
          <Box sx={{ mt: 2, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Button
              variant="contained"
              onClick={webSocket.connect}
              disabled={webSocket.isConnected}
            >
              Connect
            </Button>
            <Button
              variant="outlined"
              onClick={webSocket.disconnect}
              disabled={!webSocket.isConnected}
            >
              Disconnect
            </Button>
            <Button
              variant="contained"
              color="secondary"
              onClick={sendTestMessage}
              disabled={!webSocket.isConnected}
            >
              Send Test Message
            </Button>
            <Button
              variant="outlined"
              color="secondary"
              onClick={updatePresence}
              disabled={!webSocket.isConnected}
            >
              Update Presence
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* Auth Status */}
      {authToken && (
        <Alert severity="info" sx={{ mb: 3 }}>
          Authenticated with token: {authToken.substring(0, 20)}...
        </Alert>
      )}

      {!authToken && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          No authentication token provided
        </Alert>
      )}

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr' }, gap: 3 }}>
        {/* Recent Messages */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Recent Messages
            </Typography>
            <List dense>
              {messages.length === 0 ? (
                <ListItem>
                  <ListItemText primary="No messages yet" />
                </ListItem>
              ) : (
                messages.map((message) => (
                  <React.Fragment key={message.id}>
                    <ListItem>
                      <ListItemText
                        primary={`${message.type}: ${message.event}`}
                        secondary={
                          <Box>
                            <Typography variant="body2" component="div">
                              {typeof message.payload === 'string' 
                                ? message.payload 
                                : JSON.stringify(message.payload, null, 2)}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {new Date(message.timestamp).toLocaleTimeString()}
                            </Typography>
                          </Box>
                        }
                      />
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))
              )}
            </List>
          </CardContent>
        </Card>

        {/* Notifications */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Notifications
            </Typography>
            <List dense>
              {notifications.length === 0 ? (
                <ListItem>
                  <ListItemText primary="No notifications yet" />
                </ListItem>
              ) : (
                notifications.map((notification) => (
                  <React.Fragment key={notification.id}>
                    <ListItem>
                      <ListItemText
                        primary={notification.title}
                        secondary={
                          <Box>
                            <Typography variant="body2" component="div">
                              {notification.message}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Priority: {notification.priority} | {new Date(notification.createdAt).toLocaleTimeString()}
                            </Typography>
                          </Box>
                        }
                      />
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))
              )}
            </List>
          </CardContent>
        </Card>

        {/* Presence Updates */}
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Presence Updates
            </Typography>
            <List dense>
              {presenceUpdates.length === 0 ? (
                <ListItem>
                  <ListItemText primary="No presence updates yet" />
                </ListItem>
              ) : (
                presenceUpdates.map((presence, index) => (
                  <React.Fragment key={`${presence.userId}-${index}`}>
                    <ListItem>
                      <ListItemText
                        primary={`User ${presence.username || presence.userId}`}
                        secondary={
                          <Box>
                            <Typography variant="body2" component="div">
                              Status: {presence.status}
                            </Typography>
                            {presence.statusMessage && (
                              <Typography variant="body2" component="div">
                                Message: {presence.statusMessage}
                              </Typography>
                            )}
                            <Typography variant="caption" color="text.secondary">
                              {new Date(presence.lastSeen).toLocaleTimeString()}
                            </Typography>
                          </Box>
                        }
                      />
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))
              )}
            </List>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
};

export default WebSocketIntegrationExample;