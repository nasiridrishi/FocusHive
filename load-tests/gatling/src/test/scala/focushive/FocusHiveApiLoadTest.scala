package focushive

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * FocusHive API Load Test Simulation
 * 
 * Comprehensive load testing for FocusHive REST APIs
 * Tests authentication, hive management, presence, and analytics endpoints
 */
class FocusHiveApiLoadTest extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("focushive.baseUrl", "http://localhost:8080")
  val testDuration = System.getProperty("test.duration", "300").toInt.seconds
  val rampUpDuration = System.getProperty("ramp.duration", "60").toInt.seconds
  val users = System.getProperty("users", "10").toInt

  // HTTP Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .inferHtmlResources()
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling-FocusHive-LoadTest/1.0")
    .check(status.is(200))

  // Test Data
  val userFeeder = csv("users.csv").random
  
  // Scenarios
  object Authentication {
    val login = exec(http("Login")
      .post("/api/auth/login")
      .body(StringBody("""{"email": "${email}", "password": "${password}"}"""))
      .check(jsonPath("$.token").saveAs("authToken"))
      .check(jsonPath("$.user.id").saveAs("userId"))
    )
    
    val validateToken = exec(http("Validate Token")
      .get("/api/auth/me")
      .header("Authorization", "Bearer ${authToken}")
      .check(jsonPath("$.id").exists)
    )
  }
  
  object HiveManagement {
    val listHives = exec(http("List Hives")
      .get("/api/hives")
      .header("Authorization", "Bearer ${authToken}")
      .check(jsonPath("$").exists)
    )
    
    val createHive = exec(http("Create Hive")
      .post("/api/hives")
      .header("Authorization", "Bearer ${authToken}")
      .body(StringBody(session => {
        val timestamp = System.currentTimeMillis()
        s"""{"name": "Gatling Test Hive ${timestamp}", "description": "Automated load test hive", "category": "work", "isPublic": true, "maxMembers": 20}"""
      }))
      .check(jsonPath("$.id").saveAs("hiveId"))
    )
    
    val joinHive = exec(http("Join Hive")
      .post("/api/hives/${hiveId}/join")
      .header("Authorization", "Bearer ${authToken}")
    )
    
    val getHiveDetails = exec(http("Get Hive Details")
      .get("/api/hives/${hiveId}")
      .header("Authorization", "Bearer ${authToken}")
      .check(jsonPath("$.id").exists)
    )
  }
  
  object PresenceManagement {
    val updatePresence = exec(http("Update Presence")
      .put("/api/presence")
      .header("Authorization", "Bearer ${authToken}")
      .body(StringBody("""{"status": "online", "activity": "load-testing", "mood": "focused"}"""))
    )
    
    val getPresenceHistory = exec(http("Get Presence History")
      .get("/api/presence/history")
      .header("Authorization", "Bearer ${authToken}")
    )
  }
  
  object Analytics {
    val getDashboard = exec(http("Get Analytics Dashboard")
      .get("/api/analytics/dashboard")
      .header("Authorization", "Bearer ${authToken}")
    )
    
    val getProductivityInsights = exec(http("Get Productivity Insights")
      .get("/api/analytics/productivity")
      .header("Authorization", "Bearer ${authToken}")
    )
  }

  // User Journey Scenarios
  val normalUserJourney = scenario("Normal User Journey")
    .feed(userFeeder)
    .exec(Authentication.login)
    .pause(1, 3)
    .exec(HiveManagement.listHives)
    .pause(2, 5)
    .exec(PresenceManagement.updatePresence)
    .pause(1, 2)
    .exec(Analytics.getDashboard)
    .pause(3, 7)
    .repeat(3) {
      exec(HiveManagement.listHives)
        .pause(2, 4)
        .exec(PresenceManagement.updatePresence)
        .pause(5, 10)
    }
    
  val powerUserJourney = scenario("Power User Journey")
    .feed(userFeeder)
    .exec(Authentication.login)
    .pause(1, 2)
    .exec(HiveManagement.listHives)
    .exec(HiveManagement.createHive)
    .exec(HiveManagement.joinHive)
    .pause(1, 3)
    .exec(PresenceManagement.updatePresence)
    .exec(Analytics.getDashboard)
    .exec(Analytics.getProductivityInsights)
    .pause(2, 4)
    .repeat(5) {
      exec(HiveManagement.getHiveDetails)
        .pause(1, 2)
        .exec(PresenceManagement.updatePresence)
        .pause(3, 6)
        .exec(Analytics.getDashboard)
        .pause(2, 4)
    }

  val casualUserJourney = scenario("Casual User Journey")
    .feed(userFeeder)
    .exec(Authentication.login)
    .pause(2, 5)
    .exec(HiveManagement.listHives)
    .pause(10, 20)
    .exec(PresenceManagement.updatePresence)
    .pause(30, 60)
    .exec(Analytics.getDashboard)
    .pause(15, 30)

  // Load Test Scenarios
  setUp(
    // Normal users - 60% of load
    normalUserJourney.inject(
      rampUsers((users * 0.6).toInt) during rampUpDuration
    ),
    
    // Power users - 25% of load
    powerUserJourney.inject(
      rampUsers((users * 0.25).toInt) during rampUpDuration
    ),
    
    // Casual users - 15% of load
    casualUserJourney.inject(
      rampUsers((users * 0.15).toInt) during rampUpDuration
    )
  ).protocols(httpProtocol)
   .maxDuration(testDuration + 30.seconds) // Add buffer time
   .assertions(
     global.responseTime.percentile3.lt(1200), // 95th percentile < 1200ms
     global.responseTime.percentile4.lt(2000), // 99th percentile < 2000ms
     global.successfulRequests.percent.gt(95),  // 95% success rate
     forAll.failedRequests.percent.lt(5)        // Less than 5% failures
   )
}