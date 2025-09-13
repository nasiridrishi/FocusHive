ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .enablePlugins(GatlingPlugin)
  .settings(
    name := "focushive-gatling-tests",
    version := "1.0.0",
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.9.5" % "test",
      "io.gatling" % "gatling-test-framework" % "3.9.5" % "test",
      "com.typesafe" % "config" % "1.4.2",
      "org.slf4j" % "slf4j-simple" % "2.0.7" % "test"
    ),
    
    // Gatling configuration
    Gatling / javaOptions := Seq(
      "-Xms1G", "-Xmx2G",
      "-XX:+UseG1GC",
      "-Dfile.encoding=UTF-8",
      "-Djava.net.preferIPv4Stack=true",
      "-Djava.net.preferIPv6Addresses=false"
    ),
    
    // Test settings
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-v"),
    Test / parallelExecution := false,
    
    // Assembly settings for standalone JAR
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf" => MergeStrategy.concat
      case "reference.conf" => MergeStrategy.concat
      case _ => MergeStrategy.first
    },
    
    assembly / assemblyJarName := "focushive-gatling-tests.jar"
  )

// Gatling specific settings
enablePlugins(GatlingPlugin)

// Custom tasks for running specific test types
lazy val runApiTest = taskKey[Unit]("Run API load test")
lazy val runStressTest = taskKey[Unit]("Run stress test")  
lazy val runWebSocketTest = taskKey[Unit]("Run WebSocket test")

runApiTest := {
  (Gatling / testOnly).toTask(" focushive.FocusHiveApiLoadTest").value
}

runStressTest := {
  (Gatling / testOnly).toTask(" focushive.FocusHiveStressTest").value  
}

runWebSocketTest := {
  (Gatling / testOnly).toTask(" focushive.FocusHiveWebSocketTest").value
}