package focushive

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * FocusHive Stress Test Simulation
 * 
 * High-load stress testing to identify system breaking points
 * Progressive load increase to test system limits
 */
class FocusHiveStressTest extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("focushive.baseUrl", "http://localhost:8080")
  val testDuration = System.getProperty("test.duration", "600").toInt.seconds
  val maxUsers = System.getProperty("max.users", "200").toInt

  // HTTP Configuration with relaxed thresholds for stress testing
  val httpProtocol = http
    .baseUrl(baseUrl)
    .inferHtmlResources()
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling-FocusHive-StressTest/1.0")
    .check(status.in(200, 201, 202, 400, 401, 403, 404, 429, 500, 502, 503, 504))
    .disableFollowRedirect

  // Test Data
  val userFeeder = csv("users.csv").circular
  
  // Authentication with error handling
  val authenticateUser = exec(http("Stress Login")
    .post("/api/auth/login")
    .body(StringBody("""{"email": "${email}", "password": "${password}"}"""))
    .check(status.in(200, 401, 429, 500, 503))
    .check(
      status.is(200).transform(_.map(_ => true)).saveAs("loginSuccess"),
      jsonPath("$.token").optional.saveAs("authToken"),
      jsonPath("$.user.id").optional.saveAs("userId")
    )
  ).exitHereIfFailed

  // High-frequency API operations
  val stressApiOperations = group("Stress API Operations") {
    doIf(session => session("loginSuccess").as[Boolean]) {
      exec(
        http("Rapid Hive List")
          .get("/api/hives")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 401, 403, 429, 500, 502, 503, 504))
      ).pause(100.milliseconds, 500.milliseconds)
      .exec(
        http("Rapid Presence Update")
          .put("/api/presence")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(session => {
            val timestamp = System.currentTimeMillis()
            s"""{"status": "online", "activity": "stress-test-${timestamp}", "mood": "stressed"}"""
          }))
          .check(status.in(200, 400, 401, 403, 429, 500, 502, 503, 504))
      ).pause(50.milliseconds, 200.milliseconds)
      .exec(
        http("Rapid Dashboard Access")
          .get("/api/analytics/dashboard")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 401, 403, 429, 500, 502, 503, 504))
      )
    }
  }

  // Database stress operations
  val databaseStressOperations = group("Database Stress") {
    doIf(session => session("loginSuccess").as[Boolean]) {
      exec(
        http("Create Stress Hive")
          .post("/api/hives")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody(session => {
            val timestamp = System.currentTimeMillis()
            val userId = session("userId").as[String]
            s"""{"name": "Stress Hive ${userId}-${timestamp}", "description": "High-load stress test hive with extended description for database load", "category": "work", "isPublic": true, "maxMembers": 100}"""
          }))
          .check(
            status.in(200, 201, 400, 401, 403, 429, 500, 502, 503, 504),
            jsonPath("$.id").optional.saveAs("stressHiveId")
          )
      ).pause(200.milliseconds, 800.milliseconds)
      .doIf(session => session.contains("stressHiveId")) {
        exec(
          http("Join Stress Hive")
            .post("/api/hives/${stressHiveId}/join")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 201, 400, 401, 403, 404, 429, 500, 502, 503, 504))
        ).pause(100.milliseconds, 300.milliseconds)
        .exec(
          http("Leave Stress Hive")
            .post("/api/hives/${stressHiveId}/leave")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.in(200, 400, 401, 403, 404, 429, 500, 502, 503, 504))
        )
      }
    }
  }

  // Memory pressure operations
  val memoryPressureOperations = group("Memory Pressure") {
    doIf(session => session("loginSuccess").as[Boolean]) {
      exec(
        http("Large Data Query")
          .get("/api/analytics/productivity")
          .queryParam("period", "month")
          .queryParam("granularity", "hour")
          .queryParam("includeDetails", "true")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 401, 403, 429, 500, 502, 503, 504))
      ).pause(300.milliseconds, 1.second)
      .exec(
        http("Search Hives")
          .get("/api/hives")
          .queryParam("search", "stress test memory pressure intensive query")
          .queryParam("includeMembers", "true")
          .queryParam("includeAnalytics", "true")
          .header("Authorization", "Bearer ${authToken}")
          .check(status.in(200, 401, 403, 429, 500, 502, 503, 504))
      )
    }
  }

  // Stress test scenarios
  val lightStressScenario = scenario("Light Stress")
    .feed(userFeeder)
    .exec(authenticateUser)
    .during(testDuration) {
      exec(stressApiOperations)
        .pause(2.seconds, 5.seconds)
    }

  val mediumStressScenario = scenario("Medium Stress")
    .feed(userFeeder)
    .exec(authenticateUser)
    .during(testDuration) {
      exec(stressApiOperations)
        .pause(1.second, 3.seconds)
        .exec(databaseStressOperations)
        .pause(3.seconds, 7.seconds)
    }

  val highStressScenario = scenario("High Stress")
    .feed(userFeeder)
    .exec(authenticateUser)
    .during(testDuration) {
      randomSwitch(
        40.0 -> exec(stressApiOperations),
        30.0 -> exec(databaseStressOperations),
        30.0 -> exec(memoryPressureOperations)
      )
      .pause(500.milliseconds, 2.seconds)
    }

  val extremeStressScenario = scenario("Extreme Stress")
    .feed(userFeeder)
    .exec(authenticateUser)
    .during(testDuration) {
      exec(stressApiOperations)
        .pause(100.milliseconds, 500.milliseconds)
        .exec(databaseStressOperations)
        .pause(200.milliseconds, 800.milliseconds)
        .exec(memoryPressureOperations)
        .pause(300.milliseconds, 1.second)
    }

  // Progressive stress test setup
  setUp(
    // Phase 1: Light stress (20% of max users)
    lightStressScenario.inject(
      rampUsers((maxUsers * 0.2).toInt) during (1.minute)
    ).andThen(
      // Phase 2: Medium stress (40% of max users)
      mediumStressScenario.inject(
        rampUsers((maxUsers * 0.4).toInt) during (2.minutes)
      )
    ).andThen(
      // Phase 3: High stress (70% of max users)  
      highStressScenario.inject(
        rampUsers((maxUsers * 0.7).toInt) during (2.minutes)
      )
    ).andThen(
      // Phase 4: Extreme stress (100% of max users)
      extremeStressScenario.inject(
        rampUsers(maxUsers) during (1.minute)
      )
    )
  ).protocols(httpProtocol)
   .maxDuration(testDuration + 2.minutes) // Buffer for ramp-down
   .assertions(
     // Relaxed assertions for stress testing
     global.responseTime.percentile3.lt(5000),   // 95th percentile < 5s under stress
     global.responseTime.percentile4.lt(10000),  // 99th percentile < 10s under stress
     global.successfulRequests.percent.gt(70),   // 70% success rate acceptable under extreme stress
     global.responseTime.mean.lt(2000)           // Average response time < 2s
   )
}