import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
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
    // Spring Cloud Gateway
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    
    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // OAuth2 Resource Server & JWT
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    
    // JWT Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    
    // Redis for caching and rate limiting
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("redis.clients:jedis")
    
    // Rate Limiting - Bucket4j with Redis
    implementation("com.bucket4j:bucket4j-redis:8.7.0")
    implementation("com.bucket4j:bucket4j-core:8.7.0")
    
    // Spring Cloud LoadBalancer
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    
    // Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-reactor-resilience4j")
    
    // OpenAPI Documentation for Gateway
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")
    
    // Monitoring & Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    
    // Utilities
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.google.guava:guava:33.5.0-jre")
    
    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
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
    
    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.com.focushive.gateway", "WARN")
    systemProperty("logging.level.org.springframework", "WARN")
    
    if (System.getenv("CI") == "true") {
        maxHeapSize = "1g"
        jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
    }
}

tasks.bootJar {
    archiveFileName.set("api-gateway.jar")
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
    
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
        
        rule {
            element = "CLASS"
            excludes = listOf(
                "com.focushive.gateway.config.*",
                "com.focushive.gateway.ApiGatewayApplication*"
            )
        }
    }
}