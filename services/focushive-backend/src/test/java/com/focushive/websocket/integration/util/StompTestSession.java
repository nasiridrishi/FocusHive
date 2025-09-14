package com.focushive.websocket.integration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enhanced STOMP test session wrapper for comprehensive testing
 */
public class StompTestSession {
    
    private static final Logger log = LoggerFactory.getLogger(StompTestSession.class);
    
    private final StompSession session;
    private final Map<String, WebSocketTestClient.MessageCapture> subscriptions = new ConcurrentHashMap<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    
    public StompTestSession(StompSession session) {
        this.session = session;
        this.connected.set(session.isConnected());
        
        if (session.isConnected()) {
            connectionLatch.countDown();
        }
    }
    
    /**
     * Subscribe to destination and capture messages
     */
    public WebSocketTestClient.MessageCapture subscribe(String destination) {
        WebSocketTestClient.MessageCapture capture = new WebSocketTestClient.MessageCapture();
        
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                capture.addMessage(headers, payload);
            }
        });
        
        subscriptions.put(destination, capture);
        log.debug("Subscribed to destination: {}", destination);
        return capture;
    }
    
    /**
     * Subscribe with custom type handling
     */
    public <T> TypedMessageCapture<T> subscribe(String destination, Class<T> messageType) {
        TypedMessageCapture<T> capture = new TypedMessageCapture<>(messageType);
        
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                return messageType;
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                capture.addMessage(headers, (T) payload);
            }
        });
        
        log.debug("Subscribed to destination {} with type {}", destination, messageType.getSimpleName());
        return capture;
    }
    
    /**
     * Send message to destination
     */
    public void send(String destination, Object message) {
        session.send(destination, message);
        log.debug("Sent message to {}: {}", destination, message);
    }
    
    /**
     * Send message with headers
     */
    public void send(String destination, StompHeaders headers, Object message) {
        session.send(destination, message);
        log.debug("Sent message to {} with headers: {}", destination, message);
    }
    
    /**
     * Wait for connection to be established
     */
    public boolean waitForConnection(Duration timeout) throws InterruptedException {
        if (connected.get()) {
            return true;
        }
        
        return connectionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Check if session is connected
     */
    public boolean isConnected() {
        return session.isConnected();
    }
    
    /**
     * Get session ID
     */
    public String getSessionId() {
        return session.getSessionId();
    }
    
    /**
     * Disconnect the session
     */
    public void disconnect() {
        session.disconnect();
        connected.set(false);
        // Stop all message captures
        subscriptions.values().forEach(WebSocketTestClient.MessageCapture::stop);
        log.debug("Session disconnected: {}", session.getSessionId());
    }
    
    /**
     * Get message capture for a specific destination
     */
    public WebSocketTestClient.MessageCapture getMessageCapture(String destination) {
        return subscriptions.get(destination);
    }
    
    /**
     * Check if subscribed to destination
     */
    public boolean isSubscribedTo(String destination) {
        return subscriptions.containsKey(destination);
    }
    
    /**
     * Get number of active subscriptions
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }
    
    /**
     * Typed message capture for specific message types
     */
    public static class TypedMessageCapture<T> {
        private final Class<T> messageType;
        private final WebSocketTestClient.MessageCapture capture;
        
        public TypedMessageCapture(Class<T> messageType) {
            this.messageType = messageType;
            this.capture = new WebSocketTestClient.MessageCapture();
        }
        
        public void addMessage(StompHeaders headers, T payload) {
            capture.addMessage(headers, payload);
        }
        
        @SuppressWarnings("unchecked")
        public TypedMessage<T> waitForMessage(Duration timeout) throws InterruptedException {
            WebSocketTestClient.CapturedMessage msg = capture.waitForMessage(timeout);
            if (msg == null) {
                return null;
            }
            return new TypedMessage<>((T) msg.getPayload(), msg.getHeaders(), msg.getTimestamp());
        }
        
        @SuppressWarnings("unchecked")
        public TypedMessage<T> getNextMessage() throws InterruptedException {
            WebSocketTestClient.CapturedMessage msg = capture.getNextMessage();
            return new TypedMessage<>((T) msg.getPayload(), msg.getHeaders(), msg.getTimestamp());
        }
        
        public int getMessageCount() {
            return capture.getMessageCount();
        }
        
        public boolean hasMessages() {
            return capture.hasMessages();
        }
        
        public void stop() {
            capture.stop();
        }
        
        public void clear() {
            capture.clear();
        }
        
        public Class<T> getMessageType() {
            return messageType;
        }
    }
    
    /**
     * Typed message with payload, headers, and timestamp
     */
    public static class TypedMessage<T> {
        private final T payload;
        private final StompHeaders headers;
        private final long timestamp;
        
        public TypedMessage(T payload, StompHeaders headers, long timestamp) {
            this.payload = payload;
            this.headers = headers;
            this.timestamp = timestamp;
        }
        
        public T getPayload() { return payload; }
        public StompHeaders getHeaders() { return headers; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("TypedMessage{payload=%s, headers=%s, timestamp=%d}", 
                               payload, headers, timestamp);
        }
    }
}