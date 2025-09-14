plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.flywaydb.flyway") version "9.22.3"
    id("jacoco")
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
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // JWT Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.17.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.0")

    // Redis for caching
    implementation("redis.clients:jedis")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Circuit Breaker
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j:3.1.1")

    // Service Discovery
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.1")

    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // WireMock for external service mocking
    testImplementation("com.github.tomakehurst:wiremock-jre8:3.0.1")

    // JSON Path for response validation
    testImplementation("com.jayway.jsonpath:json-path")

    // Awaitility for asynchronous testing
    testImplementation("org.awaitility:awaitility:4.2.0")

    // Redis testing
    testImplementation("com.redis:testcontainers-redis:2.2.2")
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
    systemProperty("logging.level.com.focushive.buddy", "WARN")
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

    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("unitTest") {
    useJUnitPlatform()
    include("**/*Test.class")
    exclude("**/*IntegrationTest.class")
    exclude("**/*IT.class")

    systemProperty("spring.profiles.active", "test")
    systemProperty("logging.level.com.focushive.buddy", "WARN")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        csv.required = true
        html.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "com.focushive.buddy.BuddyServiceApplication*",
                "com.focushive.buddy.config.*Config*",
                "com.focushive.buddy.dto.*",
                "com.focushive.buddy.entity.*"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.bootJar {
    archiveFileName.set("buddy-service.jar")
}

// Flyway configuration
flyway {
    url = "jdbc:postgresql://localhost:5437/buddy_service"
    user = "buddy_user"
    password = "buddy_password"
    schemas = arrayOf("public")
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
    validateOnMigrate = true
}