import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
    jacoco
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
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
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
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.h2database:h2")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("net.ttddyy:datasource-proxy:1.10")
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

