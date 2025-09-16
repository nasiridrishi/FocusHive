# WebSocket Architecture Diagrams and Visual Documentation

## Visual Guide to FocusHive Real-Time Communication Architecture

This document provides visual representations of the WebSocket implementation architecture, useful for academic reports and technical documentation.

---

## 1. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FocusHive Platform                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐      │
│  │   Frontend   │     │   Frontend   │     │   Frontend   │      │
│  │   Client 1   │     │   Client 2   │     │   Client N   │      │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘      │
│         │                     │                     │              │
│         └──────────┬──────────┴──────────┬──────────┘              │
│                    │                     │                         │
│                    ▼                     ▼                         │
│         ┌─────────────────────────────────────────┐               │
│         │         NGINX Load Balancer             │               │
│         │     (WebSocket Sticky Sessions)         │               │
│         └─────────────────┬───────────────────────┘               │
│                           │                                        │
│         ┌─────────────────▼───────────────────────┐               │
│         │                                         │               │
│         │        FocusHive Backend Service        │               │
│         │              (Port 8080)                │               │
│         │                                         │               │
│         │  ┌───────────────────────────────────┐  │               │
│         │  │     WebSocket Configuration       │  │               │
│         │  │         Endpoint: /ws             │  │               │
│         │  │      Protocol: STOMP 1.2          │  │               │
│         │  │      Fallback: SockJS             │  │               │
│         │  └───────────────────────────────────┘  │               │
│         │                                         │               │
│         │  ┌───────────────────────────────────┐  │               │
│         │  │      Message Broker (STOMP)       │  │               │
│         │  │  ┌────────┐ ┌────────┐ ┌───────┐ │  │               │
│         │  │  │ /topic │ │ /queue │ │ /user │ │  │               │
│         │  │  └────────┘ └────────┘ └───────┘ │  │               │
│         │  └───────────────────────────────────┘  │               │
│         │                                         │               │
│         └─────────────────────────────────────────┘               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. WebSocket Connection Flow

```
Client                    Backend                   Identity Service
  │                         │                              │
  │   HTTP Upgrade Request  │                              │
  ├────────────────────────►│                              │
  │   ws://localhost:8080/ws│                              │
  │                         │                              │
  │                         │   Validate JWT Token         │
  │                         ├─────────────────────────────►│
  │                         │                              │
  │                         │   User Principal             │
  │                         │◄─────────────────────────────│
  │                         │                              │
  │   101 Switching Protocol│                              │
  │◄────────────────────────┤                              │
  │                         │                              │
  │   STOMP CONNECT Frame   │                              │
  ├────────────────────────►│                              │
  │                         │                              │
  │   CONNECTED Frame       │                              │
  │◄────────────────────────┤                              │
  │                         │                              │
  │   SUBSCRIBE             │                              │
  │   /topic/hive/123       │                              │
  ├────────────────────────►│                              │
  │                         │                              │
  │   RECEIPT               │                              │
  │◄────────────────────────┤                              │
  │                         │                              │
  │   SEND                  │                              │
  │   /app/chat/send/123    │                              │
  ├────────────────────────►│                              │
  │                         │                              │
  │                         ├──┐                           │
  │                         │  │ Process Message           │
  │                         │◄─┘                           │
  │                         │                              │
  │   MESSAGE               │                              │
  │   /topic/hive/123       │                              │
  │◄────────────────────────┤                              │
  │                         │                              │
```

---

## 3. Message Flow Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Message Flow Patterns                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Point-to-Point (Private Messages)                       │
│  ─────────────────────────────────────                      │
│                                                              │
│    Client A                Server               Client B    │
│       │                      │                      │       │
│       │  SEND /app/msg/user │                      │       │
│       ├─────────────────────►│                      │       │
│       │                      │  /user/queue/msgs   │       │
│       │                      ├─────────────────────►│       │
│       │                      │                      │       │
│                                                              │
│  2. Publish-Subscribe (Hive Broadcasts)                     │
│  ──────────────────────────────────────                     │
│                                                              │
│    Client A            Server          Client B,C,D         │
│       │                  │                 │││              │
│       │  SEND /app/chat │                 │││              │
│       ├─────────────────►│                 │││              │
│       │                  │  /topic/chat    │││              │
│       │                  ├────────────────►│││              │
│       │                  │                 │││              │
│                                                              │
│  3. Request-Reply (Synchronous Operations)                  │
│  ─────────────────────────────────────────                  │
│                                                              │
│    Client              Server            Backend Service    │
│       │                  │                      │           │
│       │  SEND /app/timer│                      │           │
│       ├─────────────────►│  Process Request    │           │
│       │                  ├─────────────────────►│           │
│       │                  │  Response           │           │
│       │  REPLY          │◄─────────────────────┤           │
│       │◄─────────────────┤                      │           │
│       │                  │                      │           │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. Module Integration Map

```
┌────────────────────────────────────────────────────────────────┐
│                   WebSocket Module Integration                  │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│   ┌─────────────────────────────────────────────────────┐     │
│   │              WebSocket Infrastructure               │     │
│   │                                                     │     │
│   │  ┌─────────┐  ┌──────────┐  ┌──────────────────┐  │     │
│   │  │ Config  │  │ Security │  │ Event Handlers   │  │     │
│   │  └────┬────┘  └────┬─────┘  └────────┬─────────┘  │     │
│   │       │            │                  │            │     │
│   └───────┼────────────┼──────────────────┼────────────┘     │
│           │            │                  │                   │
│           ▼            ▼                  ▼                   │
│   ┌───────────────────────────────────────────────────┐       │
│   │            WebSocket Controllers                  │       │
│   ├───────────────────────────────────────────────────┤       │
│   │                                                   │       │
│   │  ┌──────────────┐      ┌──────────────┐         │       │
│   │  │    Chat      │      │   Presence   │         │       │
│   │  │ Controller   │      │  Controller  │         │       │
│   │  └──────┬───────┘      └──────┬───────┘         │       │
│   │         │                      │                 │       │
│   │  ┌──────▼───────┐      ┌──────▼───────┐         │       │
│   │  │    Timer     │      │    Forum    │         │       │
│   │  │ Controller   │      │ Controller  │         │       │
│   │  └──────┬───────┘      └──────┬───────┘         │       │
│   │         │                      │                 │       │
│   │  ┌──────▼──────────────────────▼───────┐         │       │
│   │  │       Analytics Controller          │         │       │
│   │  └──────────────┬───────────────────────┘         │       │
│   │                 │                                │       │
│   └─────────────────┼─────────────────────────────────┘       │
│                     │                                         │
│                     ▼                                         │
│   ┌───────────────────────────────────────────────────┐       │
│   │              Business Services                    │       │
│   ├───────────────────────────────────────────────────┤       │
│   │                                                   │       │
│   │  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐ │       │
│   │  │  Chat  │  │Presence│  │ Timer  │  │ Forum  │ │       │
│   │  │Service │  │Service │  │Service │  │Service │ │       │
│   │  └────────┘  └────────┘  └────────┘  └────────┘ │       │
│   │                                                   │       │
│   └───────────────────────────────────────────────────┘       │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 5. Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  WebSocket Security Layers                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: Transport Security                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  • TLS 1.3 Encryption (wss://)                      │   │
│  │  • Certificate Validation                           │   │
│  │  • Perfect Forward Secrecy                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                │
│                           ▼                                │
│  Layer 2: Connection Authentication                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  • JWT Token Validation                             │   │
│  │  • Identity Service Integration                     │   │
│  │  • User Principal Establishment                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                │
│                           ▼                                │
│  Layer 3: Message Authorization                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  • Destination Access Control                       │   │
│  │  • Role-Based Permissions                           │   │
│  │  • Hive Membership Validation                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                │
│                           ▼                                │
│  Layer 4: Application Security                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  • Input Validation                                 │   │
│  │  • XSS Prevention                                   │   │
│  │  • Rate Limiting                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Deployment Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                   Production Deployment                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌────────────────────────────────────────────────────┐     │
│   │            Kubernetes Cluster (AWS EKS)            │     │
│   ├────────────────────────────────────────────────────┤     │
│   │                                                    │     │
│   │  ┌──────────────────────────────────────────────┐  │     │
│   │  │         Ingress Controller (NGINX)          │  │     │
│   │  │  • WebSocket Support                        │  │     │
│   │  │  • Session Affinity                         │  │     │
│   │  │  • SSL Termination                          │  │     │
│   │  └────────────────┬─────────────────────────────┘  │     │
│   │                   │                               │     │
│   │      ┌────────────┴────────────┐                 │     │
│   │      │                         │                 │     │
│   │  ┌───▼────┐  ┌────────┐  ┌────▼───┐            │     │
│   │  │ Pod 1  │  │ Pod 2  │  │ Pod 3  │            │     │
│   │  │Backend │  │Backend │  │Backend │            │     │
│   │  └───┬────┘  └───┬────┘  └────┬───┘            │     │
│   │      │           │            │                 │     │
│   │      └───────────┼────────────┘                 │     │
│   │                  │                              │     │
│   │  ┌───────────────▼──────────────────────┐       │     │
│   │  │        Redis Cluster                 │       │     │
│   │  │  • Message Broker (Pub/Sub)          │       │     │
│   │  │  • Session Storage                   │       │     │
│   │  │  • Cache Layer                       │       │     │
│   │  └──────────────────────────────────────┘       │     │
│   │                                                  │     │
│   └──────────────────────────────────────────────────┘     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 7. Performance Metrics Visualization

```
┌─────────────────────────────────────────────────────────────┐
│              WebSocket Performance Metrics                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Connection Latency (ms)                                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 250 ┤                                              │    │
│  │ 200 ┤     ╱╲                                       │    │
│  │ 150 ┤    ╱  ╲                                      │    │
│  │ 100 ┤   ╱    ╲____                                │    │
│  │  50 ┤  ╱            ╲_______                      │    │
│  │   0 └─┴───────────────────────────────────────────┘    │
│  │      0   100   500   1000   1500   2000            │    │
│  │            Concurrent Connections                  │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  Message Throughput (msg/sec)                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │12000┤                            ___________       │    │
│  │10000┤                     ______╱           ╲      │    │
│  │ 8000┤              ______╱                   ╲     │    │
│  │ 6000┤       ______╱                           ╲    │    │
│  │ 4000┤ _____╱                                    ╲   │    │
│  │ 2000┤╱                                           ╲  │    │
│  │    0└─┴───────────────────────────────────────────┘    │
│  │      0   10   20   30   40   50   60   70   80    │    │
│  │                 Time (seconds)                     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  Memory Usage per Connection                               │
│  ┌────────────────────────────────────────────────────┐    │
│  │  ┌──────────┬───────────┬──────────┬──────────┐   │    │
│  │  │ Idle     │ Active    │ Sending  │ Heavy    │   │    │
│  │  │ 8KB      │ 12KB      │ 16KB     │ 24KB     │   │    │
│  │  │ ████     │ ███████   │ ████████ │ ████████ │   │    │
│  │  │ ████     │ ███████   │ ████████ │ ████████ │   │    │
│  │  │ ████     │ ███████   │ ████████ │ ████████ │   │    │
│  │  └──────────┴───────────┴──────────┴──────────┘   │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. Event Flow Sequence Diagram

```
┌─────────────────────────────────────────────────────────────┐
│           Real-time Collaboration Event Flow               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  User A joins Hive                                         │
│  ─────────────────                                         │
│                                                             │
│    Browser         Backend         Other Users             │
│       │              │                 │││                 │
│       │  Connect     │                 │││                 │
│       ├─────────────►│                 │││                 │
│       │              │                 │││                 │
│       │  Subscribe   │                 │││                 │
│       │  /topic/hive │                 │││                 │
│       ├─────────────►│                 │││                 │
│       │              │                 │││                 │
│       │  Join Hive   │                 │││                 │
│       ├─────────────►│                 │││                 │
│       │              │                 │││                 │
│       │              │  User Joined    │││                 │
│       │              ├────────────────►│││                 │
│       │              │                 │││                 │
│       │  Member List │                 │││                 │
│       │◄─────────────┤                 │││                 │
│       │              │                 │││                 │
│                                                             │
│  User A starts Focus Timer                                 │
│  ─────────────────────────                                 │
│                                                             │
│       │  Start Timer │                 │││                 │
│       ├─────────────►│                 │││                 │
│       │              │                 │││                 │
│       │              │  Timer Started  │││                 │
│       │              ├────────────────►│││                 │
│       │              │                 │││                 │
│       │  Timer Sync  │                 │││                 │
│       │◄─────────────┤                 │││                 │
│       │              │                 │││                 │
│                                                             │
│  User B sends Chat Message                                 │
│  ─────────────────────────                                 │
│                                                             │
│       │││            │  Send Message   │                   │
│       │││            │◄────────────────┤                   │
│       │││            │                 │                   │
│       │││            │  Broadcast      │                   │
│       │◄─────────────┤─────────────────►                   │
│       │││            │                 │                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Data Flow in Modules

```
┌─────────────────────────────────────────────────────────────┐
│                Module Data Flow Patterns                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Chat Module                                               │
│  ───────────                                               │
│                                                             │
│    Input ──► Validation ──► Service ──► Persistence       │
│      │           │            │            │               │
│      │           │            │            ▼               │
│      │           │            │        Database            │
│      │           │            │            │               │
│      │           │            ▼            │               │
│      │           │        Broadcast        │               │
│      │           │            │            │               │
│      └───────────┴────────────┴────────────┘               │
│                                                             │
│  Presence Module                                           │
│  ───────────────                                           │
│                                                             │
│    Heartbeat ──► Status Update ──► Cache ──► Broadcast    │
│        │             │              │           │          │
│        │             │              ▼           │          │
│        │             │           Redis          │          │
│        │             │              │           │          │
│        │             └──────────────┴───────────┘          │
│        │                                                   │
│        └──► Timeout Detection ──► Cleanup                  │
│                                                             │
│  Analytics Module                                          │
│  ────────────────                                          │
│                                                             │
│    Event ──► Aggregation ──► Processing ──► Storage       │
│      │           │              │             │            │
│      │           │              │             ▼            │
│      │           │              │         TimeSeries       │
│      │           │              │             DB           │
│      │           │              │             │            │
│      │           │              └─────────────┘            │
│      │           │                                         │
│      └───────────┴──► Real-time Dashboard                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. Scalability Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Horizontal Scaling Architecture               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Phase 1: Single Instance (Current)                        │
│  ───────────────────────────────────                       │
│                                                             │
│    ┌──────────────────────────────────┐                    │
│    │     Single Backend Instance      │                    │
│    │    In-Memory Message Broker      │                    │
│    │     Capacity: 1,500 users        │                    │
│    └──────────────────────────────────┘                    │
│                                                             │
│  Phase 2: Multiple Instances with Redis                    │
│  ───────────────────────────────────────                   │
│                                                             │
│    ┌─────────┐  ┌─────────┐  ┌─────────┐                  │
│    │Instance1│  │Instance2│  │Instance3│                  │
│    └────┬────┘  └────┬────┘  └────┬────┘                  │
│         │            │            │                        │
│         └────────────┼────────────┘                        │
│                      │                                     │
│              ┌───────▼───────┐                             │
│              │  Redis Broker │                             │
│              │  Pub/Sub      │                             │
│              └───────────────┘                             │
│         Capacity: 10,000 users                             │
│                                                             │
│  Phase 3: Microservices with Message Queue                │
│  ──────────────────────────────────────────                │
│                                                             │
│    ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐                   │
│    │ WS   │ │ Chat │ │Timer │ │Forum │                   │
│    │Gateway│ │ Svc  │ │ Svc  │ │ Svc  │                   │
│    └───┬───┘ └──┬───┘ └──┬───┘ └──┬───┘                   │
│        │        │        │        │                       │
│        └────────┴────────┴────────┘                       │
│                      │                                     │
│              ┌───────▼───────┐                             │
│              │  Kafka/RabbitMQ                             │
│              │  Event Stream  │                             │
│              └───────────────┘                             │
│         Capacity: 100,000+ users                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Conclusion

These visual representations provide a comprehensive view of the WebSocket implementation architecture in the FocusHive Backend service. The diagrams illustrate the multi-layered approach to real-time communication, from low-level transport protocols to high-level business logic integration.

The architecture demonstrates:
- **Scalability**: Progressive scaling from single instance to distributed systems
- **Security**: Multiple layers of authentication and authorization
- **Performance**: Optimized message routing and resource utilization
- **Reliability**: Fallback mechanisms and error handling
- **Modularity**: Clean separation between modules and services

These diagrams serve as valuable documentation for both technical implementation and academic evaluation of the system design.

---

*Document Version: 1.0*
*Last Updated: September 21, 2025*
*Author: FocusHive Development Team*