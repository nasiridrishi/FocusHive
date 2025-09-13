package focushive

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import scala.concurrent.duration._

/**
 * FocusHive WebSocket Load Test Simulation
 * 
 * Tests WebSocket connections and real-time message handling
 * Simulates presence updates, chat messages, and notifications
 */
class FocusHiveWebSocketTest extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("focushive.baseUrl", "http://localhost:8080")
  val wsUrl = System.getProperty("focushive.wsUrl", "ws://localhost:8080")
  val testDuration = System.getProperty("test.duration", "300").toInt.seconds
  val users = System.getProperty("users", "15").toInt

  // HTTP Configuration for authentication
  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling-FocusHive-WebSocketTest/1.0")

  // WebSocket Configuration
  val wsProtocol = ws
    .baseUrl(wsUrl)
    .reconnect
    .maxReconnects(3)

  // Test Data
  val userFeeder = csv("users.csv").circular
  
  // Authentication
  val authenticate = exec(http("WebSocket Auth Login")
    .post("/api/auth/login")
    .body(StringBody("""{"email": "${email}", "password": "${password}"}"""))
    .check(
      status.is(200),
      jsonPath("$.token").saveAs("authToken"),
      jsonPath("$.user.id").saveAs("userId")
    )
  )

  // WebSocket connection with authentication
  val connectWebSocket = exec(ws("Connect WebSocket")
    .connect("/ws")
    .queryParam("token", "${authToken}")
    .await(5.seconds)(
      ws.checkTextMessage("WebSocket Connected")
        .matching(jsonPath("$.type").is("connection-established"))
    )
  )

  // Real-time presence operations
  val presenceOperations = group("Presence Operations") {
    exec(ws("Send Presence Update")
      .sendText(session => {
        val userId = session("userId").as[String]
        val timestamp = System.currentTimeMillis()
        s"""{"type": "presence-update", "userId": "$userId", "status": "online", "activity": "websocket-testing", "timestamp": $timestamp}"""
      })
    ).pause(2.seconds, 5.seconds)
    .exec(ws("Send Activity Change")
      .sendText(session => {
        val userId = session("userId").as[String]
        val timestamp = System.currentTimeMillis()
        val activities = Array("coding", "designing", "meeting", "break", "studying")
        val activity = activities(scala.util.Random.nextInt(activities.length))
        s"""{"type": "presence-update", "userId": "$userId", "status": "busy", "activity": "$activity", "timestamp": $timestamp}"""
      })
    )
  }

  // Chat message operations
  val chatOperations = group("Chat Operations") {
    exec(ws("Send Chat Message")
      .sendText(session => {
        val userId = session("userId").as[String]
        val timestamp = System.currentTimeMillis()
        val messageId = scala.util.Random.nextInt(1000)
        s"""{"type": "chat-message", "senderId": "$userId", "hiveId": "websocket-test-hive", "message": "WebSocket load test message #$messageId", "timestamp": $timestamp}"""
      })
    ).pause(3.seconds, 8.seconds)
    .exec(ws("Send Chat Reaction")
      .sendText(session => {
        val userId = session("userId").as[String]
        val timestamp = System.currentTimeMillis()
        val reactions = Array("👍", "👎", "❤️", "😂", "😮", "😢")
        val reaction = reactions(scala.util.Random.nextInt(reactions.length))
        s"""{"type": "chat-reaction", "senderId": "$userId", "messageId": "msg-${scala.util.Random.nextInt(100)}", "reaction": "$reaction", "timestamp": $timestamp}"""
      })
    )
  }

  // Notification operations
  val notificationOperations = group("Notification Operations") {
    exec(ws("Send Notification Ack")
      .sendText(session => {
        val userId = session("userId").as[String]
        val timestamp = System.currentTimeMillis()
        s"""{"type": "notification-ack", "userId": "$userId", "notificationId": "notif-${scala.util.Random.nextInt(100)}", "timestamp": $timestamp}"""
      })
    )
  }

  // Heartbeat operations
  val heartbeatOperations = group("Heartbeat Operations") {
    exec(ws("Send Heartbeat")
      .sendText(session => {
        val userId = session("userId").as[String]
        val timestamp = System.currentTimeMillis()
        s"""{"type": "heartbeat", "userId": "$userId", "timestamp": $timestamp}"""
      })
    )
  }

  // Message listeners
  val messageHandlers = exec(
    ws.checkTextMessage("Handle Incoming Messages")
      .check(
        jsonPath("$.type").saveAs("messageType"),
        jsonPath("$.timestamp").optional.saveAs("messageTimestamp")
      )
      .silent // Don't log every message to reduce noise
  )

  // WebSocket scenarios
  val lightWebSocketUser = scenario("Light WebSocket User")
    .feed(userFeeder)
    .exec(authenticate)
    .exec(connectWebSocket)
    .during(testDuration) {
      exec(presenceOperations)
        .pause(30.seconds, 60.seconds)
        .exec(heartbeatOperations)
        .pause(15.seconds, 30.seconds)
    }
    .exec(ws("Close WebSocket Connection").close)

  val activeWebSocketUser = scenario("Active WebSocket User")
    .feed(userFeeder)
    .exec(authenticate)
    .exec(connectWebSocket)
    .during(testDuration) {
      randomSwitch(
        40.0 -> exec(presenceOperations),
        35.0 -> exec(chatOperations),
        15.0 -> exec(notificationOperations),
        10.0 -> exec(heartbeatOperations)
      )
      .pause(5.seconds, 15.seconds)
    }
    .exec(ws("Close Active WebSocket").close)

  val heavyWebSocketUser = scenario("Heavy WebSocket User")
    .feed(userFeeder)
    .exec(authenticate)
    .exec(connectWebSocket)
    .during(testDuration) {
      exec(presenceOperations)
        .pause(2.seconds, 5.seconds)
        .exec(chatOperations)
        .pause(3.seconds, 7.seconds)
        .exec(notificationOperations)
        .pause(1.second, 3.seconds)
        .exec(heartbeatOperations)
        .pause(8.seconds, 15.seconds)
    }
    .exec(ws("Close Heavy WebSocket").close)

  // Connection stability test
  val connectionStabilityUser = scenario("Connection Stability User")
    .feed(userFeeder)
    .exec(authenticate)
    .exec(connectWebSocket)
    .during(testDuration) {
      // Long-lived connection with periodic activity
      exec(heartbeatOperations)
        .pause(30.seconds)
        .exec(presenceOperations)
        .pause(2.minutes)
        .exec(chatOperations)
        .pause(1.minute)
    }
    .exec(ws("Close Stable Connection").close)

  // High-frequency message test
  val highFrequencyUser = scenario("High Frequency User")
    .feed(userFeeder)
    .exec(authenticate)
    .exec(connectWebSocket)
    .during(testDuration) {
      exec(presenceOperations)
        .pause(1.second, 3.seconds)
        .exec(chatOperations)
        .pause(2.seconds, 4.seconds)
        .exec(heartbeatOperations)
        .pause(5.seconds, 10.seconds)
    }
    .exec(ws("Close High Frequency Connection").close)

  // Setup with mixed user behaviors
  setUp(
    // Light users - 40% of load
    lightWebSocketUser.inject(
      rampUsers((users * 0.4).toInt) during (30.seconds)
    ),
    
    // Active users - 30% of load
    activeWebSocketUser.inject(
      rampUsers((users * 0.3).toInt) during (45.seconds)
    ),
    
    // Heavy users - 15% of load
    heavyWebSocketUser.inject(
      rampUsers((users * 0.15).toInt) during (60.seconds)
    ),
    
    // Connection stability users - 10% of load
    connectionStabilityUser.inject(
      rampUsers((users * 0.1).toInt) during (20.seconds)
    ),
    
    // High frequency users - 5% of load
    highFrequencyUser.inject(
      rampUsers((users * 0.05).toInt) during (90.seconds)
    )
  ).protocols(httpProtocol, wsProtocol)
   .maxDuration(testDuration + 2.minutes)
   .assertions(
     // WebSocket specific assertions
     global.responseTime.percentile3.lt(200),    // 95th percentile < 200ms for WebSocket messages
     global.responseTime.percentile4.lt(500),    // 99th percentile < 500ms for WebSocket messages  
     global.successfulRequests.percent.gt(95),   // 95% success rate for WebSocket operations
     forAll.failedRequests.percent.lt(5),        // Less than 5% WebSocket failures
     
     // Connection stability assertions
     details("Connect WebSocket").successfulRequests.percent.gt(98), // 98% connection success rate
     details("Send Heartbeat").responseTime.mean.lt(50),            // Average heartbeat < 50ms
     details("Send Presence Update").responseTime.mean.lt(100)       // Average presence update < 100ms
   )
}