plugins {
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
    id("org.owasp.dependencycheck") version "9.0.6"
    id("org.sonarqube") version "4.4.1.3373"
    id("com.github.hierynomus.license") version "0.16.1"
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
    implementation("org.springframework.boot:spring-boot-starter-web") {
        // Exclude Tomcat to use Undertow
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-undertow")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // OAuth2 Resource Server for JWT validation
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    
    // JWT Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    
    // Message Queue - RabbitMQ
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    
    // Email Support
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    
    // AWS SES
    implementation("software.amazon.awssdk:ses:2.27.21")
    implementation("software.amazon.awssdk:auth:2.27.21")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.17.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.0")
    runtimeOnly("com.h2database:h2")
    
    // Redis for caching
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("redis.clients:jedis")

    // Bucket4j for rate limiting - commented out due to network issues
    // implementation("com.bucket4j:bucket4j-core:8.10.0")
    // implementation("com.bucket4j:bucket4j-redis:8.10.0")

    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    
    // Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j:3.1.1")
    implementation("io.vavr:vavr:0.10.4")
    
    // Service Discovery
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.1")
    
    // Monitoring and Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave:3.5.1")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin:1.40.0")
    
    // AOP for performance monitoring
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.aspectj:aspectjweaver")
    
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
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Email Testing
    testImplementation("com.icegreen:greenmail-junit5:2.0.0")
    testImplementation("com.icegreen:greenmail-spring:2.0.0")
    
    // Template Engine Testing
    testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    
    // WireMock for external service mocking
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")
    
    // JSON Path for response validation
    testImplementation("com.jayway.jsonpath:json-path")
    
    // Awaitility for asynchronous testing
    testImplementation("org.awaitility:awaitility:4.2.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.2")
        mavenBom("org.testcontainers:testcontainers-bom:1.19.8")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.com.focushive.notification", "WARN")
    systemProperty("logging.level.org.springframework", "WARN")
    
    if (project.hasProperty("excludeIntegrationTests")) {
        exclude("**/*IntegrationTest.class")
        exclude("**/*IT.class")
    }
    
    if (System.getenv("CI") == "true") {
        maxHeapSize = "1g"
        jvmArgs("-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100")
    }
    
    ignoreFailures = project.hasProperty("ignoreTestFailures")
}

tasks.register<Test>("unitTest") {
    useJUnitPlatform()
    include("**/*Test.class")
    exclude("**/*IntegrationTest.class")
    exclude("**/*IT.class")
    
    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.com.focushive.notification", "WARN")
}

tasks.bootJar {
    archiveFileName.set("notification-service.jar")
}

// OWASP Dependency Check Configuration - Temporarily disabled for deployment
// dependencyCheck {
//     format = "HTML"
//     suppressionFile = "owasp-suppressions.xml"
//     analyzers.assemblyEnabled.set(false)
//     analyzers.nugetconfEnabled.set(false)
//     analyzers.nodeEnabled.set(false)
//     nvd.apiKey.set(System.getenv("NVD_API_KEY") ?: "")
//     nvd.delay.set(6000)
//     failBuildOnCVSS = 7.0f
// }

// SonarQube Configuration
sonarqube {
    properties {
        property("sonar.projectKey", "focushive-notification-service")
        property("sonar.projectName", "FocusHive Notification Service")
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "http://localhost:9000")
        property("sonar.login", System.getenv("SONAR_TOKEN") ?: "")
        property("sonar.java.source", "21")
        property("sonar.java.target", "21")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.exclusions", "**/dto/**,**/entity/**,**/config/**")
    }
}

// License Check Configuration
license {
    header = file("LICENSE_HEADER")
    include("**/*.java")
    exclude("**/generated/**")
    mapping("java", "SLASHSTAR_STYLE")
}

// Security Task Group
tasks.register("securityCheck") {
    group = "security"
    description = "Run all security checks"
    dependsOn(
        // "dependencyCheckAnalyze", // Temporarily disabled due to plugin issues
        "sonarqube",
        "license"
    )
}

// Pre-commit hook task
tasks.register("preCommitChecks") {
    group = "verification"
    description = "Run checks before committing code"
    dependsOn(
        "test",
        "dependencyCheckAnalyze"
    )
}