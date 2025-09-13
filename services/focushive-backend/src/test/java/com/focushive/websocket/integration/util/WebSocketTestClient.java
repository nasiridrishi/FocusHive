package com.focushive.websocket.integration.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Utility class for WebSocket integration testing
 */
public class WebSocketTestClient {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketTestClient.class);
    
    private final WebSocketStompClient stompClient;
    private final String url;
    private final Duration connectionTimeout;
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    
    public WebSocketTestClient(String url) {
        this(url, Duration.ofSeconds(10));
    }
    
    public WebSocketTestClient(String url, Duration connectionTimeout) {
        this.url = url;
        this.connectionTimeout = connectionTimeout;
        
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        this.stompClient.setDefaultHeartbeat(new long[]{10000, 10000});
        this.stompClient.setTaskScheduler(createTaskScheduler());
    }
    
    /**
     * Connect to WebSocket with authentication headers
     */
    public StompSession connect(StompHeaders connectHeaders) throws Exception {
        StompSessionHandler sessionHandler = new TestStompSessionHandler();
        ListenableFuture<StompSession> future = stompClient.connect(url, null, connectHeaders, sessionHandler);
        
        try {
            return future.get(connectionTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("WebSocket connection timed out after " + connectionTimeout.toSeconds() + " seconds", e);
        }
    }
    
    /**
     * Connect with JWT token
     */
    public StompSession connectWithJwt(String jwtToken) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);
        return connect(connectHeaders);
    }
    
    /**
     * Connect without authentication (for testing auth failures)
     */
    public StompSession connectUnauthenticated() throws Exception {
        return connect(new StompHeaders());
    }
    
    /**
     * Subscribe to a destination with message capture
     */
    public MessageCapture subscribe(StompSession session, String destination) {
        MessageCapture capture = new MessageCapture();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                capture.addMessage(headers, payload);
                log.debug("Received message on {}: {}", destination, payload);
            }
        });
        return capture;
    }
    
    /**
     * Subscribe with custom frame handler
     */
    public StompSession.Subscription subscribe(StompSession session, String destination, 
                                             StompFrameHandler handler) {
        return session.subscribe(destination, handler);
    }
    
    /**
     * Send message to destination
     */
    public void sendMessage(StompSession session, String destination, Object message) {
        session.send(destination, message);
        messageCounter.incrementAndGet();
        log.debug("Sent message to {}: {}", destination, message);
    }
    
    /**
     * Send message with headers
     */
    public void sendMessage(StompSession session, String destination, StompHeaders headers, Object message) {
        session.send(destination, headers, message);
        messageCounter.incrementAndGet();
        log.debug("Sent message to {} with headers {}: {}", destination, headers, message);
    }
    
    /**
     * Wait for connection to be established
     */
    public boolean waitForConnection(StompSession session, Duration timeout) {
        long startTime = System.currentTimeMillis();
        while (!session.isConnected() && 
               (System.currentTimeMillis() - startTime) < timeout.toMillis()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return session.isConnected();
    }
    
    /**
     * Wait for subscription to be active
     */
    public boolean waitForSubscription(StompSession.Subscription subscription, Duration timeout) {
        // Spring STOMP doesn't provide direct subscription status
        // We rely on the receipt mechanism for acknowledgment
        try {
            Thread.sleep(500); // Give time for subscription to be processed
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Disconnect cleanly
     */
    public void disconnect(StompSession session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.debug("WebSocket session disconnected");
        }
    }
    
    /**
     * Close client and cleanup resources
     */
    public void shutdown() {
        // Stop the STOMP client
        stompClient.stop();
    }
    
    public int getMessagesSent() {
        return messageCounter.get();
    }
    
    private TaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("websocket-test-");
        scheduler.initialize();
        return scheduler;
    }
    
    /**
     * Test STOMP session handler
     */
    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.debug("WebSocket connected: {}", session.getSessionId());
        }
        
        @Override
        public void handleException(StompSession session, StompCommand command, 
                                  StompHeaders headers, byte[] payload, Throwable exception) {
            log.error("WebSocket error in session {}: {}", session.getSessionId(), exception.getMessage());
        }
        
        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.error("WebSocket transport error in session {}: {}", session.getSessionId(), exception.getMessage());
        }
    }
    
    /**
     * Message capture utility for testing
     */
    public static class MessageCapture {
        private final BlockingQueue<CapturedMessage> messages = new LinkedBlockingQueue<>();
        private final AtomicBoolean active = new AtomicBoolean(true);
        
        public void addMessage(StompHeaders headers, Object payload) {
            if (active.get()) {
                messages.offer(new CapturedMessage(headers, payload));
            }
        }
        
        public CapturedMessage waitForMessage(Duration timeout) throws InterruptedException {
            return messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        public CapturedMessage getNextMessage() throws InterruptedException {
            return messages.take();
        }
        
        public int getMessageCount() {
            return messages.size();
        }
        
        public boolean hasMessages() {
            return !messages.isEmpty();
        }
        
        public void stop() {
            active.set(false);
        }
        
        public void clear() {
            messages.clear();
        }
    }
    
    /**
     * Captured message with headers and payload
     */
    public static class CapturedMessage {
        private final StompHeaders headers;
        private final Object payload;
        private final long timestamp;
        
        public CapturedMessage(StompHeaders headers, Object payload) {
            this.headers = headers;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
        }
        
        public StompHeaders getHeaders() { return headers; }
        public Object getPayload() { return payload; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("CapturedMessage{headers=%s, payload=%s, timestamp=%d}", 
                               headers, payload, timestamp);
        }
    }
}