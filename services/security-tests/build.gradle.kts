plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("jacoco")
}

group = "com.focushive"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Test dependencies
    implementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    
    // Test Containers for integration tests
    implementation("org.testcontainers:junit-jupiter")
    implementation("org.testcontainers:postgresql")
    implementation("org.testcontainers:testcontainers")
    
    // Security testing
    implementation("org.springframework.security:spring-security-test")
    
    // JWT testing
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // HTTP client for security testing
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:mockwebserver")
    
    // OWASP Java Encoder for XSS prevention testing
    implementation("org.owasp.encoder:encoder:1.2.3")
    
    // Database for testing
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    
    // Redis for testing
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("it.ozimov:embedded-redis:0.7.3")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Wiremock for mocking external services
    implementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    
    // For SQL injection testing
    implementation("net.sf.jsqlparser:jsqlparser:4.7")
    
    // For performance and load testing
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic")
    
    // Commons utilities
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-codec:commons-codec")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.19.7")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Enable parallel execution for faster tests
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    
    // Security test specific properties
    systemProperty("spring.profiles.active", "test")
    systemProperty("security.test.mode", "true")
    
    // JVM arguments for security testing
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED"
    )
}

// JaCoCo configuration for test coverage
jacoco {
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

// Custom task for security tests only
tasks.register<Test>("securityTest") {
    description = "Runs only security-related tests"
    group = "verification"
    
    include("**/security/**")
    include("**/*Security*Test.class")
    include("**/*OWASP*Test.class")
    
    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/security"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/security"))
    }
}

// Task for OWASP vulnerability tests
tasks.register<Test>("owaspTest") {
    description = "Runs OWASP Top 10 vulnerability tests"
    group = "verification"
    
    include("**/*OWASP*Test.class")
    include("**/vulnerability/**")
    
    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/owasp"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/owasp"))
    }
}

// Task for penetration testing scenarios
tasks.register<Test>("penTest") {
    description = "Runs penetration testing scenarios"
    group = "verification"
    
    include("**/*PenTest.class")
    include("**/penetration/**")
    
    // Increase timeout for penetration tests
    timeout.set(Duration.ofMinutes(30))
    
    reports {
        html.outputLocation.set(layout.buildDirectory.dir("reports/tests/pen"))
        junitXml.outputLocation.set(layout.buildDirectory.dir("test-results/pen"))
    }
}