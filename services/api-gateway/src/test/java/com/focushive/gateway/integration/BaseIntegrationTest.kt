package com.focushive.gateway.integration

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*

/**
 * Base integration test class for API Gateway tests
 * 
 * Provides:
 * - TestContainers for Redis (rate limiting)
 * - JWT token generation utilities
 * - WebTestClient configuration
 * - Common test setup
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected var port: Int = 0

    protected lateinit var webTestClient: WebTestClient

    companion object {
        private const val JWT_SECRET = "your-256-bit-secret-key-here-make-it-secure-for-testing"
        private val JWT_KEY = Keys.hmacShaKeyFor(JWT_SECRET.toByteArray(StandardCharsets.UTF_8))

        @Container
        @JvmStatic
        val redisContainer: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("focushive.jwt.secret") { JWT_SECRET }
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            redisContainer.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            redisContainer.stop()
        }
    }

    protected fun createWebTestClient(): WebTestClient {
        return WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:$port/api")
            .responseTimeout(Duration.ofSeconds(30))
            .build()
    }

    protected fun createValidJwtToken(
        userId: String = "test-user-id",
        username: String = "testuser",
        roles: List<String> = listOf("USER"),
        personaId: String = "test-persona"
    ): String {
        return Jwts.builder()
            .setSubject(userId)
            .claim("username", username)
            .claim("roles", roles)
            .claim("persona_id", personaId)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600000)) // 1 hour
            .signWith(JWT_KEY)
            .compact()
    }

    protected fun createExpiredJwtToken(
        userId: String = "test-user-id",
        username: String = "testuser"
    ): String {
        return Jwts.builder()
            .setSubject(userId)
            .claim("username", username)
            .claim("roles", listOf("USER"))
            .setIssuedAt(Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
            .setExpiration(Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
            .signWith(JWT_KEY)
            .compact()
    }

    protected fun createInvalidJwtToken(): String {
        // Create a token with wrong signature
        val wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing-invalid-tokens".toByteArray())
        return Jwts.builder()
            .setSubject("test-user-id")
            .claim("username", "testuser")
            .claim("roles", listOf("USER"))
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(wrongKey)
            .compact()
    }

    protected fun withBearerToken(token: String): String {
        return "Bearer $token"
    }

    protected fun isRedisAvailable(): Boolean {
        return try {
            redisContainer.isRunning
        } catch (e: Exception) {
            false
        }
    }
}