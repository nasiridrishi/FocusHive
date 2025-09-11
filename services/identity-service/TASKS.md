# Identity Service - Test Coverage Improvement Plan

## Current Status
- **Current Coverage**: 56%
- **Target Coverage**: 80%+
- **Total Tests**: 1238
- **Passing Tests**: 1144 (92%)
- **Failing Tests**: 94
- **Skipped Tests**: 30

## Phase 1: Fix Failing Tests (Target: +10-15% coverage)
Priority: CRITICAL - These tests already exist but aren't passing

### 1.1 OAuth2AuthorizationServerIntegrationTest (16 failures)
- [ ] Fix test OAuth2 client configuration
- [ ] Implement proper authorization server setup for tests
- [ ] Fix authorization code flow tests
- [ ] Fix client credentials flow tests
- [ ] Fix refresh token flow tests
- [ ] Fix token introspection tests
- [ ] Fix token revocation tests
- [ ] Fix user info endpoint tests
- [ ] Fix JWK Set endpoint tests
- [ ] Fix server metadata tests
- [ ] Verify all OAuth2 flows work end-to-end

### 1.2 AuthControllerTest (33 failures)
- [ ] Fix JWT token generation in test context
- [ ] Fix authentication service mocking
- [ ] Fix login endpoint tests
- [ ] Fix registration endpoint tests
- [ ] Fix token refresh tests
- [ ] Fix token validation tests
- [ ] Fix token introspection tests
- [ ] Fix password reset flow tests
- [ ] Fix persona switching tests
- [ ] Fix logout endpoint tests
- [ ] Ensure proper security context setup

### 1.3 PersonaControllerTest (14 failures)
- [ ] Fix persona creation tests
- [ ] Fix template-based persona creation
- [ ] Fix active persona retrieval
- [ ] Fix persona switching logic
- [ ] Fix persona deletion tests
- [ ] Fix authentication principal handling
- [ ] Set up proper test fixtures

### 1.4 Configuration Tests (5 failures)
- [ ] Fix ApplicationConfigTest password encoder tests
- [ ] Fix IdentityServiceApplicationTests context loading
- [ ] Ensure all beans are properly configured

### 1.5 SimplePerformanceTestControllerTest (2 failures)
- [ ] Fix basic performance endpoint tests
- [ ] Fix debug endpoint tests

## Phase 2: Service Layer Testing (Target: +15-20% coverage)
Priority: HIGH - Core business logic

### CRITICAL APPROACH: Write ONE test → Run it → Fix it → Verify it passes → THEN move to next test
**DO NOT write multiple tests at once. Each test must pass before writing the next one.**

### 2.1 AuthService Testing (Write → Test → Fix → Verify for EACH)
- [ ] Write test: User registration with valid data → Run → Fix → Verify ✓
- [ ] Write test: User registration with duplicate email → Run → Fix → Verify ✓
- [ ] Write test: User registration with invalid email → Run → Fix → Verify ✓
- [ ] Write test: Login with valid credentials → Run → Fix → Verify ✓
- [ ] Write test: Login with invalid credentials → Run → Fix → Verify ✓
- [ ] Write test: JWT token generation → Run → Fix → Verify ✓
- [ ] Write test: JWT token validation → Run → Fix → Verify ✓
- [ ] Write test: Token refresh with valid token → Run → Fix → Verify ✓
- [ ] Write test: Token refresh with expired token → Run → Fix → Verify ✓
- [ ] Write test: Password reset request → Run → Fix → Verify ✓
- [ ] Write test: Password reset completion → Run → Fix → Verify ✓
- [ ] Write test: Account lockout after 5 failed attempts → Run → Fix → Verify ✓
- [ ] Write test: Session creation → Run → Fix → Verify ✓
- [ ] Write test: Session invalidation → Run → Fix → Verify ✓

### 2.2 OAuth2AuthorizationService Testing (Write → Test → Fix → Verify for EACH)
- [ ] Write test: Generate authorization code → Run → Fix → Verify ✓
- [ ] Write test: Validate PKCE challenge → Run → Fix → Verify ✓
- [ ] Write test: Authenticate client with secret → Run → Fix → Verify ✓
- [ ] Write test: Authenticate client with JWT → Run → Fix → Verify ✓
- [ ] Write test: Validate requested scopes → Run → Fix → Verify ✓
- [ ] Write test: Handle consent approval → Run → Fix → Verify ✓
- [ ] Write test: Exchange code for token → Run → Fix → Verify ✓
- [ ] Write test: Rotate refresh tokens → Run → Fix → Verify ✓
- [ ] Write test: Revoke access token → Run → Fix → Verify ✓
- [ ] Write test: Register new client → Run → Fix → Verify ✓
- [ ] Write test: Update client settings → Run → Fix → Verify ✓
- [ ] Write test: Delete client → Run → Fix → Verify ✓

### 2.3 PersonaService Testing (Write → Test → Fix → Verify for EACH)
- [ ] Write test: Create persona with valid data → Run → Fix → Verify ✓
- [ ] Write test: Create persona from template → Run → Fix → Verify ✓
- [ ] Write test: Switch to different persona → Run → Fix → Verify ✓
- [ ] Write test: Get active persona → Run → Fix → Verify ✓
- [ ] Write test: Update persona privacy settings → Run → Fix → Verify ✓
- [ ] Write test: Prevent default persona deletion → Run → Fix → Verify ✓
- [ ] Write test: Set default persona → Run → Fix → Verify ✓
- [ ] Write test: Log persona activity → Run → Fix → Verify ✓
- [ ] Write test: Delete non-default persona → Run → Fix → Verify ✓

### 2.4 PrivacyService Testing (Write → Test → Fix → Verify for EACH)
- [ ] Write test: Update privacy preferences → Run → Fix → Verify ✓
- [ ] Write test: Request data export → Run → Fix → Verify ✓
- [ ] Write test: Process data export → Run → Fix → Verify ✓
- [ ] Write test: Request account deletion → Run → Fix → Verify ✓
- [ ] Write test: Manage consent settings → Run → Fix → Verify ✓
- [ ] Write test: Apply data retention policy → Run → Fix → Verify ✓
- [ ] Write test: GDPR data portability → Run → Fix → Verify ✓

## Phase 3: Integration Testing (Target: +5-10% coverage)
Priority: MEDIUM - End-to-end workflows

### 3.1 Complete OAuth2 Flows
- [ ] Authorization code with PKCE full flow
- [ ] Client credentials flow for M2M
- [ ] Refresh token rotation flow
- [ ] Device authorization flow
- [ ] Token introspection across services
- [ ] Dynamic client registration

### 3.2 Authentication Scenarios
- [ ] User registration → login → logout flow
- [ ] Password reset email → token → new password
- [ ] Failed login → account lock → unlock
- [ ] Session timeout and renewal
- [ ] Concurrent session handling

### 3.3 Persona Workflows
- [ ] Create persona → activate → switch → delete
- [ ] Template persona creation and customization
- [ ] Multiple persona management
- [ ] Persona-based authorization

### 3.4 Privacy Operations
- [ ] User requests data export → process → deliver
- [ ] Delete account → cascade deletions → confirmation
- [ ] Update privacy settings → apply across services

## Phase 4: Error Handling & Edge Cases (Target: +3-5% coverage)
Priority: LOW - Defensive programming

### 4.1 Exception Scenarios
- [ ] Database connection failures
- [ ] External service timeouts
- [ ] Invalid input validation
- [ ] Concurrent modification exceptions
- [ ] Transaction rollback scenarios

### 4.2 Security Edge Cases
- [ ] SQL injection attempts
- [ ] XSS attack vectors
- [ ] CSRF token validation
- [ ] Rate limiting enforcement
- [ ] Brute force protection

### 4.3 Data Validation
- [ ] Email format validation
- [ ] Password strength requirements
- [ ] Username uniqueness
- [ ] Phone number formats
- [ ] Date/time boundaries

## Phase 5: Configuration & Infrastructure (Target: +2-3% coverage)
Priority: LOWEST - Framework code

### 5.1 Security Configuration
- [ ] Test security filter chains
- [ ] Test CORS configuration
- [ ] Test security headers
- [ ] Test authentication providers

### 5.2 OAuth2 Configuration  
- [ ] Test authorization server settings
- [ ] Test JWK source configuration
- [ ] Test token customizers
- [ ] Test client repository

### 5.3 Infrastructure
- [ ] Test cache configuration
- [ ] Test async configuration
- [ ] Test scheduled tasks
- [ ] Test event listeners

## Testing Strategy

### Approach
1. **Fix First**: Repair all failing tests before adding new ones
2. **High Value**: Focus on business logic over configuration
3. **Realistic**: Don't aim for 100% - focus on meaningful coverage
4. **Maintainable**: Write clear, documented, maintainable tests

### Tools & Techniques
- **Unit Tests**: Mockito for isolation
- **Integration Tests**: @SpringBootTest with test containers
- **Data Tests**: @DataJpaTest with H2
- **Web Tests**: @WebMvcTest with MockMvc
- **Performance Tests**: JMH for benchmarking

### Quality Standards
- Each test must be independent
- Use builders for test data
- Assert both positive and negative cases
- Include edge cases and error paths
- Document complex test scenarios

## Success Metrics
- [ ] 80%+ overall test coverage
- [ ] 100% coverage of critical business logic
- [ ] 0 failing tests in CI/CD pipeline
- [ ] <10 second total test execution time
- [ ] All security vulnerabilities tested

## Timeline
- Phase 1: 2-3 days (Critical - must complete)
- Phase 2: 3-4 days (High priority)
- Phase 3: 2 days (Medium priority)
- Phase 4: 1 day (Low priority)
- Phase 5: 1 day (Lowest priority)

Total: ~10-12 days for 80%+ coverage