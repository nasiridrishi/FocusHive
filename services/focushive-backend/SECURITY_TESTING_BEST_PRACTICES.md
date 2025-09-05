# Security Testing Best Practices for FocusHive Backend

This document outlines the best practices for testing security features in the FocusHive backend service, based on the successful resolution of authentication and authorization test issues.

## Overview

The FocusHive backend uses Spring Security 6.x with JWT token authentication and @AuthenticationPrincipal for accessing authenticated user details in controllers. This document provides guidance for properly testing these security features.

## Key Security Testing Components

### 1. Test Security Configuration

**File**: `/src/test/java/com/focushive/config/TestWebMvcSecurityConfig.java`

This is the core test configuration that enables @AuthenticationPrincipal support in controller tests.

```java
@TestConfiguration
@EnableWebSecurity
@Profile("webmvc-test")
public class TestWebMvcSecurityConfig {
    
    @Bean
    @Primary
    public SecurityFilterChain webMvcTestSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        UserDetails user = User.builder()
            .username("testuser")
            .password("{noop}password")
            .roles("USER")
            .build();
        
        UserDetails admin = User.builder()
            .username("adminuser")
            .password("{noop}adminpassword")
            .roles("USER", "ADMIN")
            .build();

        return new InMemoryUserDetailsManager(user, admin);
    }
}
```

**Key Points**:
- Uses `@Profile("webmvc-test")` to avoid conflicts with main application
- Provides in-memory UserDetailsService with test users
- Configures security rules matching the main application
- Uses `{noop}` password encoder for simplicity in tests

### 2. Controller Test Pattern

For controller tests that use `@AuthenticationPrincipal`, use this pattern:

```java
@SpringBootTest(classes = {YourController.class, TestWebMvcSecurityConfig.class})
@ImportAutoConfiguration({
    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class,
    org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration.class,
    org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration.class
})
@AutoConfigureMockMvc
@ActiveProfiles("webmvc-test")
@WithMockUser(username = "testuser", roles = "USER")
class YourControllerTest {
    // Test methods
}
```

**Key Changes from @WebMvcTest**:
- Use `@SpringBootTest` instead of `@WebMvcTest`
- Import only necessary auto-configurations
- Use `TestWebMvcSecurityConfig` for authentication support
- Apply `@WithMockUser` at class or method level
- Use `"webmvc-test"` profile

### 3. Authentication Test Patterns

#### Class-level Authentication
```java
@WithMockUser(username = "testuser", roles = "USER")
class ControllerTest {
    // All tests run with this user context
}
```

#### Method-level Authentication
```java
@Test
@WithMockUser(username = "adminuser", roles = {"USER", "ADMIN"})
void adminOnlyEndpoint_withAdminRole_shouldSucceed() throws Exception {
    // Test admin functionality
}
```

#### Testing Unauthenticated Access
```java
@Test
void protectedEndpoint_withoutAuth_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/v1/protected"))
           .andExpect(status().isUnauthorized());
}
```

## Fixed Test Examples

### 1. TimerControllerTest
- **Issue**: @AuthenticationPrincipal not working with @WebMvcTest
- **Solution**: Switched to @SpringBootTest with TestWebMvcSecurityConfig
- **Result**: All 11 tests passing

### 2. PresenceRestControllerTest
- **Issue**: Security context not properly configured
- **Solution**: Applied authentication to all endpoints via @WithMockUser
- **Result**: All 11 tests passing

### 3. ChatRestControllerTest
- **Issue**: @WebMvcTest not loading security context
- **Solution**: Same pattern as other controller tests
- **Result**: All 6 tests passing

### 4. AnalyticsControllerIntegrationTest
- **Issue**: Complex feature flag configuration with authentication
- **Solution**: Combined @SpringBootTest pattern with existing test properties
- **Result**: All 5 tests passing

## Security Test Categories

### 1. Authentication Tests
- Test valid JWT token processing
- Test invalid/expired token handling
- Test @AuthenticationPrincipal injection
- Test user details extraction

**Example**:
```java
@Test
@WithMockUser(username = "testuser")
void getCurrentUser_withValidAuth_returnsUserDetails() {
    // Test that @AuthenticationPrincipal UserDetails works
}
```

### 2. Authorization Tests
- Test role-based access control
- Test endpoint-level permissions
- Test admin vs. user access

**Example**:
```java
@Test
@WithMockUser(roles = "ADMIN")
void adminEndpoint_withAdminRole_succeeds() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users"))
           .andExpect(status().isOk());
}

@Test
@WithMockUser(roles = "USER")
void adminEndpoint_withUserRole_forbidden() throws Exception {
    mockMvc.perform(get("/api/v1/admin/users"))
           .andExpect(status().isForbidden());
}
```

### 3. CSRF and CORS Tests
- Test CSRF protection (disabled for API)
- Test CORS configuration
- Test request headers handling

### 4. Security Configuration Tests
- Test security filter chain configuration
- Test public vs. protected endpoints
- Test authentication entry points

## Common Pitfalls and Solutions

### 1. @WebMvcTest vs @SpringBootTest
**Problem**: @WebMvcTest doesn't load full security context needed for @AuthenticationPrincipal

**Solution**: Use @SpringBootTest with minimal configuration:
```java
@SpringBootTest(classes = {Controller.class, TestWebMvcSecurityConfig.class})
```

### 2. Missing UserDetailsService
**Problem**: @WithMockUser requires a UserDetailsService bean

**Solution**: Provide in-memory UserDetailsService in TestWebMvcSecurityConfig

### 3. Profile Conflicts
**Problem**: Test security config conflicts with main application

**Solution**: Use dedicated test profile (`"webmvc-test"`)

### 4. Cache Clearing in Integration Tests
**Problem**: Redis cache operations fail with mock connections

**Solution**: Wrap cache operations in try-catch blocks:
```java
try {
    cache.clear();
} catch (Exception e) {
    // Ignore cache clearing errors in test environment
}
```

## Integration Test Considerations

For complex integration tests that require external service mocking (e.g., Identity Service):
- Use WireMock for external service simulation
- Provide comprehensive mock configurations
- Handle cache management carefully with mixed mock/real components
- Consider using TestContainers for more realistic testing

## Performance and Maintainability

### Test Performance
- Use minimal Spring Boot configuration in tests
- Avoid loading unnecessary components
- Use `@DirtiesContext` sparingly
- Profile tests to identify slow components

### Test Maintainability
- Extract common test configurations to reusable classes
- Use consistent naming patterns for test methods
- Document complex test setups
- Keep security configurations in sync with main application

## Security Test Checklist

Before adding new controller endpoints:

- [ ] Add authentication tests with @WithMockUser
- [ ] Test unauthorized access returns 401/403
- [ ] Verify @AuthenticationPrincipal injection works
- [ ] Test role-based authorization if applicable
- [ ] Add integration tests for complex security flows
- [ ] Update security configuration documentation

## Tools and Libraries Used

- **Spring Security Test**: `@WithMockUser`, `@WithAnonymousUser`
- **Spring Boot Test**: `@SpringBootTest`, `@AutoConfigureMockMvc`
- **MockMvc**: HTTP request simulation
- **WireMock**: External service mocking (for integration tests)
- **TestContainers**: Real database testing (optional)

## Conclusion

Following these patterns ensures:
1. Consistent and reliable security testing
2. Proper authentication and authorization coverage
3. Maintainable test code
4. Production-ready security validation

The key insight is using @SpringBootTest with minimal configuration instead of @WebMvcTest for controllers that use @AuthenticationPrincipal, combined with a dedicated test security configuration.