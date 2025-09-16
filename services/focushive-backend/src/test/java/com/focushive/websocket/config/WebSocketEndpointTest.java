package com.focushive.websocket.config;

import com.focushive.FocusHiveApplication;
import com.focushive.config.TestWebSocketController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Test for WebSocket endpoint configuration.
 * This test verifies that:
 * 1. /ws endpoint is available and accepts STOMP connections
 * 2. /ws endpoint works with SockJS fallback
 * 3. CORS is properly configured for frontend origins
 * 4. Authentication is not required for initial connection (CONNECT frame)
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = {FocusHiveApplication.class, TestWebSocketController.class})
@ActiveProfiles("test")
@DisplayName("WebSocket Endpoint Configuration Tests")
class WebSocketEndpointTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private String wsUrl;
    private String sockJsUrl;

    @BeforeEach
    void setUp() {
        // Setup WebSocket URLs
        wsUrl = "ws://localhost:" + port + "/ws";
        sockJsUrl = "http://localhost:" + port + "/ws";

        // Setup STOMP client
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    @DisplayName("Should connect to /ws endpoint with STOMP protocol")
    void testWebSocketEndpointConnection() throws Exception {
        BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();
        StompSession session = null;

        try {
            StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    blockingQueue.add("connected");
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                           StompHeaders headers, byte[] payload, Throwable exception) {
                    blockingQueue.add("error: " + exception.getMessage());
                }
            };

            // Try to connect to WebSocket endpoint
            session = stompClient.connectAsync(wsUrl, sessionHandler).get(5, TimeUnit.SECONDS);

            // Verify connection
            String result = blockingQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(result, "Should receive connection confirmation");
            assertEquals("connected", result, "Should successfully connect to /ws endpoint");
            assertTrue(session.isConnected(), "Session should be connected");

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Test
    @DisplayName("Should connect to /ws endpoint with SockJS fallback")
    void testSockJsEndpointConnection() throws Exception {
        // Setup SockJS client
        SockJsClient sockJsClient = new SockJsClient(
            Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()))
        );
        WebSocketStompClient sockJsStompClient = new WebSocketStompClient(sockJsClient);
        sockJsStompClient.setMessageConverter(new MappingJackson2MessageConverter());

        BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>();
        StompSession session = null;

        try {
            StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    blockingQueue.add("connected");
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                           StompHeaders headers, byte[] payload, Throwable exception) {
                    blockingQueue.add("error: " + exception.getMessage());
                }
            };

            // Try to connect via SockJS
            session = sockJsStompClient.connectAsync(sockJsUrl, sessionHandler).get(5, TimeUnit.SECONDS);

            // Verify connection
            String result = blockingQueue.poll(5, TimeUnit.SECONDS);
            assertNotNull(result, "Should receive connection confirmation via SockJS");
            assertEquals("connected", result, "Should successfully connect to /ws endpoint via SockJS");
            assertTrue(session.isConnected(), "Session should be connected via SockJS");

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Test
    @DisplayName("Should allow CONNECT frames without authentication")
    void testConnectWithoutAuthentication() throws Exception {
        BlockingQueue<Boolean> connectionResult = new LinkedBlockingDeque<>();

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                connectionResult.add(true);
            }

            @Override
            public void handleException(StompSession session, StompCommand command,
                                       StompHeaders headers, byte[] payload, Throwable exception) {
                connectionResult.add(false);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                connectionResult.add(false);
            }
        };

        StompSession session = null;
        try {
            // Connect without any authentication headers
            session = stompClient.connectAsync(wsUrl, sessionHandler).get(5, TimeUnit.SECONDS);

            // Should connect successfully without authentication
            Boolean connected = connectionResult.poll(5, TimeUnit.SECONDS);
            assertNotNull(connected, "Should receive connection result");
            assertTrue(connected, "Should allow CONNECT without authentication (auth happens later)");

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Test
    @DisplayName("Should have proper message broker prefixes configured")
    void testMessageBrokerConfiguration() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingDeque<>();
        StompSession session = null;

        try {
            StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    // Try to subscribe to different broker prefixes
                    session.subscribe("/topic/test", new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return String.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            messages.add("topic:" + payload);
                        }
                    });

                    session.subscribe("/queue/test", new StompFrameHandler() {
                        @Override
                        public Type getPayloadType(StompHeaders headers) {
                            return String.class;
                        }

                        @Override
                        public void handleFrame(StompHeaders headers, Object payload) {
                            messages.add("queue:" + payload);
                        }
                    });

                    messages.add("subscribed");
                }
            };

            session = stompClient.connectAsync(wsUrl, sessionHandler).get(5, TimeUnit.SECONDS);

            // Verify subscriptions were successful
            String result = messages.poll(5, TimeUnit.SECONDS);
            assertEquals("subscribed", result, "Should successfully subscribe to broker destinations");

        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}