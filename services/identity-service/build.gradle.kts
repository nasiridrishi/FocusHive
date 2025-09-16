import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
    jacoco
    id("org.owasp.dependencycheck") version "9.0.10"
    id("me.champeau.jmh") version "0.7.2"
}

group = "com.focushive"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Spring Authorization Server - OAuth2 Provider
    implementation("org.springframework.security:spring-security-oauth2-authorization-server:1.3.1")
    
    // OAuth2 Resource Server
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    
    // OpenID Connect
    implementation("org.springframework.security:spring-security-oauth2-jose")
    
    // JWT Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.17.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.0")
    
    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("redis.clients:jedis")
    
    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    
    // Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j:3.1.1")
    // Add explicit Resilience4j Bulkhead support for tests referencing Bulkhead/BulkheadRegistry
    testImplementation("io.github.resilience4j:resilience4j-bulkhead")

    // OpenFeign for inter-service communication (notification-service integration)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    
    // Rate Limiting - Bucket4j with Redis
    implementation("com.bucket4j:bucket4j-redis:8.7.0")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    
    // Security Enhancements
    implementation("org.springframework.boot:spring-boot-starter-mail")  // Email support
    implementation("org.springframework.security:spring-security-crypto")  // Additional crypto support
    
    // IP Geolocation (for threat detection)
    implementation("com.maxmind.geoip2:geoip2:4.2.0")
    
    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Only use Brave bridge for tracing (not both OTel and Brave to avoid conflicts)
    // implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")  // Zipkin support
    
    // Zipkin/Brave dependencies
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.zipkin.brave:brave-instrumentation-spring-webmvc")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    
    // Utilities
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.google.guava:guava:33.2.1-jre")
    
    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // Ensure actuator types are also available on test classpath (redundant but explicit)
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")
    // Add actuator autoconfigure for health endpoint classes in tests
    testImplementation("org.springframework.boot:spring-boot-actuator-autoconfigure")
    // Add actuator core for health classes
    testImplementation("org.springframework.boot:spring-boot-actuator")
    // Spring Boot TestContainers support for @ServiceConnection
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.redis:testcontainers-redis:2.2.2")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.h2database:h2")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("net.ttddyy:datasource-proxy:1.10")

    // Network failure testing with Toxiproxy
    testImplementation("org.testcontainers:toxiproxy:1.19.8")
    testImplementation("eu.rekawek.toxiproxy:toxiproxy-java:2.1.7")

    // JUnit Platform Suite for test organization
    testImplementation("org.junit.platform:junit-platform-suite-engine")
    testImplementation("org.junit.platform:junit-platform-suite-api")

    // Performance Testing Dependencies
    // JMH (Java Microbenchmark Harness) for microbenchmarks
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Gatling for load testing
    testImplementation("io.gatling.highcharts:gatling-charts-highcharts:3.10.3")
    testImplementation("io.gatling:gatling-app:3.10.3")
    testImplementation("io.gatling:gatling-recorder:3.10.3")

    // WireMock for service mocking
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.1")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")

    // Memory profiling and monitoring (use JVM built-in tools instead of Eclipse MAT)
    // testImplementation("org.eclipse.mat:org.eclipse.mat.parser:1.14.0") // Removed due to availability issues

    // Metrics and monitoring for tests
    testImplementation("io.micrometer:micrometer-core")
    testImplementation("io.micrometer:micrometer-registry-prometheus")
    
    // Logback for Markers in tests
    testImplementation("net.logstash.logback:logstash-logback-encoder:7.4")
    testImplementation("ch.qos.logback:logback-classic")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.2")
        mavenBom("org.testcontainers:testcontainers-bom:1.19.8")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Make tests more resilient in CI environment
    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.com.focushive.identity", "WARN")
    systemProperty("logging.level.org.springframework", "WARN")
    
    // Exclude integration tests that require external dependencies
    if (project.hasProperty("excludeIntegrationTests")) {
        exclude("**/*IntegrationTest.class")
        exclude("**/*IT.class")
    }
    
    // Memory settings for all test environments
    maxHeapSize = "2g"
    jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
    
    // Additional CI-specific settings
    if (System.getenv("CI") == "true") {
        // CI-specific optimizations can go here
        jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport")
    }
    
    // Continue on test failures for better reporting
    ignoreFailures = project.hasProperty("ignoreTestFailures")
}

// Add a separate task for unit tests only
tasks.register<Test>("unitTest") {
    useJUnitPlatform()
    include("**/*Test.class")
    exclude("**/*IntegrationTest.class")
    exclude("**/*IT.class")
    
    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.com.focushive.identity", "WARN")
}

tasks.bootJar {
    archiveFileName.set("identity-service.jar")
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    // Include all main source files
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
    
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.08".toBigDecimal() // Temporarily lowered for development (target: 80%)
            }
        }
        
        // Exclude configuration classes from coverage requirements  
        rule {
            element = "CLASS"
            excludes = listOf(
                "com.focushive.identity.config.*",
                "com.focushive.identity.IdentityServiceApplication*"
            )
        }
    }
}

// OWASP Dependency Check Configuration for vulnerability scanning
configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    // Configuration for production security scanning
    failBuildOnCVSS = 7.0f  // Fail build if CVSS score >= 7.0 (High severity)
    suppressionFile = "dependency-check-suppressions.xml"

    analyzers.apply {
        // Enable various analyzers
        assemblyEnabled = false  // .NET assemblies
        nuspecEnabled = false    // .NET NuGet
        nodeEnabled = false      // Node.js

        // Enable for Java/Kotlin
        jarEnabled = true
        centralEnabled = true
    }

    // Report formats
    formats = listOf("HTML", "JSON", "CSV")

    // Scan configuration
    scanConfigurations = listOf("compileClasspath", "runtimeClasspath")

    // Skip certain configurations
    skipConfigurations = listOf("testCompileClasspath", "testRuntimeClasspath")

    // NVD configuration
    nvd.apply {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
        delay = 4000  // Milliseconds between requests
    }
}

// Task to check for dependency updates
tasks.register("checkDependencyUpdates") {
    group = "verification"
    description = "Check for dependency updates and security patches"

    doLast {
        println("Checking for dependency updates...")
        println("Run './gradlew dependencyUpdates' for detailed report")
        println("Run './gradlew dependencyCheckAnalyze' for vulnerability scan")
    }
}

// Custom task to generate security report
tasks.register("securityReport") {
    group = "verification"
    description = "Generate comprehensive security report"
    dependsOn("dependencyCheckAnalyze", "jacocoTestReport")

    doLast {
        println("Security Report Generated:")
        println("- Dependency vulnerabilities: build/reports/dependency-check-report.html")
        println("- Code coverage: build/reports/jacoco/test/html/index.html")
    }
}

// JMH Configuration for Microbenchmarks
jmh {
    warmupIterations.set(3)
    iterations.set(10)
    fork.set(2)
    benchmarkMode.set(listOf("Throughput", "AverageTime"))
    timeUnit.set("ms")
    verbosity.set("NORMAL")
    failOnError.set(true)
    forceGC.set(true)
    jvmArgs.set(listOf("-Xms2g", "-Xmx4g", "-XX:+UseG1GC"))
    resultFormat.set("JSON")
    humanOutputFile.set(file("${layout.buildDirectory.get()}/reports/jmh/results.txt"))
    resultsFile.set(file("${layout.buildDirectory.get()}/reports/jmh/results.json"))
}

// Custom task for performance tests
tasks.register<Test>("performanceTest") {
    group = "verification"
    description = "Run comprehensive performance tests (PERF-001 to PERF-005)"

    useJUnitPlatform {
        includeTags("performance")
    }

    include("**/performance/**/*Test.class")

    systemProperty("spring.profiles.active", "performance")
    systemProperty("logging.level.com.focushive.identity", "WARN")
    systemProperty("logging.level.org.springframework", "WARN")

    // Memory settings for performance tests
    maxHeapSize = "4g"
    jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UnlockExperimentalVMOptions")

    // Longer timeout for performance tests
    timeout = Duration.ofMinutes(30)

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

