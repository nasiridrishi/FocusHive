plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.flywaydb.flyway") version "9.22.3"
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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    
    // Redis
    implementation("redis.clients:jedis")
    
    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    
    // Utilities
    implementation("org.apache.commons:commons-lang3:3.13.0")
    
    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Flyway configuration
flyway {
    url = "jdbc:postgresql://localhost:5436/forum_service"
    user = "forum_user"
    password = "forum_password"
    schemas = arrayOf("public")
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
    validateOnMigrate = true
}