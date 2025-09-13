plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
}

group = "com.focushive"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Test Starters
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.springframework.security:spring-security-test")
    
    // TestContainers
    testImplementation("org.testcontainers:junit-jupiter:1.19.0")
    testImplementation("org.testcontainers:postgresql:1.19.0")
    testImplementation("com.redis:testcontainers-redis:1.6.4")
    
    // REST Assured for API Testing
    testImplementation("io.rest-assured:rest-assured:5.3.1")
    testImplementation("io.rest-assured:json-path:5.3.1")
    testImplementation("io.rest-assured:xml-path:5.3.1")
    testImplementation("io.rest-assured:spring-mock-mvc:5.3.1")
    
    // WireMock for external service mocking
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    
    // Awaitility for async testing
    testImplementation("org.awaitility:awaitility:4.2.0")
    
    // JSON processing
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // WebSocket testing support
    testImplementation("org.springframework:spring-websocket")
    testImplementation("org.springframework:spring-messaging")
    
    // MockMvc and Hamcrest
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Performance and memory settings for integration tests
    minHeapSize = "512m"
    maxHeapSize = "2g"
    
    // Test execution settings
    systemProperty("spring.profiles.active", "integration-test")
    systemProperty("testcontainers.reuse.enable", "false")
    
    // Parallel execution for faster test runs
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    
    // Detailed test reporting
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

// Task to run only cross-service integration tests
tasks.register<Test>("crossServiceIntegrationTest") {
    description = "Runs cross-service data flow integration tests"
    group = "verification"
    
    useJUnitPlatform {
        includeEngines("junit-jupiter")
        includeTags("cross-service", "data-flow", "integration")
    }
    
    // Extended timeout for complex cross-service tests
    systemProperty("junit.jupiter.execution.timeout.default", "5m")
}

// Task to run performance-focused integration tests
tasks.register<Test>("performanceIntegrationTest") {
    description = "Runs performance and load integration tests"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("performance", "load", "concurrency")
    }
}