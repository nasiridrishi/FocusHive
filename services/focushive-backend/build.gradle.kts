import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.focushive"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.1"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-messaging")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    
    // Distributed Tracing & Observability
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-observation")
    implementation("org.apache.commons:commons-pool2:2.12.0")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    
    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // JWT - Keep for token parsing, but not generation
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    
    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")  // Temporary for quick startup
    implementation("org.flywaydb:flyway-core:10.17.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.0")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")
    
    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Rate Limiting with Bucket4j
    implementation("com.bucket4j:bucket4j-redis:8.7.0")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    
    // Redis Jedis client
    implementation("redis.clients:jedis:5.1.0")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
    // Redis testing dependencies
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("it.ozimov:embedded-redis:0.7.3") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    testImplementation("org.awaitility:awaitility:4.2.0")
    testRuntimeOnly("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
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
    systemProperty("logging.level.com.focushive", "WARN")
    systemProperty("logging.level.org.springframework", "WARN")
    
    // Exclude integration tests that require external dependencies
    if (project.hasProperty("excludeIntegrationTests")) {
        exclude("**/*IntegrationTest.class")
        exclude("**/*IT.class")
    }
    
    // CI-specific settings
    if (System.getenv("CI") == "true") {
        maxHeapSize = "1g"
        jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
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
    systemProperty("logging.level.com.focushive", "WARN")
}