# JWT Token Validation – RSA/HMAC Mismatch

## Observed Symptoms

- **All authenticated routes return 401** even immediately after `POST /api/v1/auth/register` issues access/refresh tokens. The same happens when the token is sent in cookies.
- `/api/v1/auth/refresh` and `/api/v1/auth/validate` respond `500`, and `/api/v1/auth/introspect` logs `io.jsonwebtoken.security.InvalidKeyException: RS256 verification keys must be PublicKeys (implement java.security.PublicKey). Provided key type: javax.crypto.spec.SecretKeySpec.` (see container log snippet at `2025-09-20T14:04:28Z`).

These failures halt end-to-end testing because every secured controller ultimately depends on `JwtTokenProvider` to authenticate the request.

## Root Cause

1. **Tokens are signed with RSA** – `RSAJwtTokenProvider.generateAccessToken` signs with the active RSA key pair (`src/main/java/com/focushive/identity/security/RSAJwtTokenProvider.java:189-217`).
2. **Validation still uses the HMAC secret** – the inherited `JwtTokenProvider.extractAllClaims` (`src/main/java/com/focushive/identity/security/JwtTokenProvider.java:186-204`) parses the token with `verifyWith(secretKey)` where `secretKey` is a `SecretKeySpec`. When the parser sees the `RS256` header it throws `InvalidKeyException` because a symmetric key cannot verify an RSA signature.
3. **RSA overrides only the `validateToken` path** – `RSAJwtTokenProvider.validateToken` detects the `RS*` header and verifies with every stored public key (`src/.../RSAJwtTokenProvider.java:407-436`). However helper methods such as `extractUserId`, `extractPersonaId`, `extractExpiration`, and the introspection flow still call the parent implementation, triggering the exception above.

Because the failure happens inside `extractAllClaims`, the authentication filter swallows the exception and the request remains unauthenticated, producing 401 responses across the board.

## Remediation Options

### Option A – Extend RSA implementation to use RSA for claim extraction (Recommended)

Modify `RSAJwtTokenProvider` so every method that reads claims uses the same RSA-aware parser logic:

1. **Override `extractAllClaims`** to call a new private `parseClaimsWithRSA(token)` that iterates the known public keys and returns the first successful parse.
2. **Override helper methods** (`extractUserId`, `extractPersonaId`, `extractExpiration`, `extractIssuedAt`, `extractEmail`, `extractClaim`) to make sure they delegate to the RSA parser instead of the HMAC implementation. Alternatively, update the base class so that it defers to an injectable `JwtParser` strategy chosen per algorithm.
3. **Add defensive logging** when no RSA keys validate the token so operators can rotate keys before old tokens become useless.

To guard against regression, add an integration test that:

- Boots the application with `jwt.use-rsa=true`.
- Calls registration, captures the access token, and hits a protected endpoint asserting `200`.
- Exercises `/api/v1/auth/introspect` to ensure the claim extraction path works with RSA.

### Option B – Run in HMAC-only mode (Short-term fallback)

If you need an immediate unblock before code changes:

1. Set `jwt.use-rsa=false` in the environment, ensuring `JWT_SECRET` is present and strong (≥32 characters).
2. Restart the service so both signing and verification rely on the symmetric secret. Tokens issued under RSA will become invalid, so rotate them during deployment.
3. Re-enable RSA once Option A is implemented.

### Option C – Hybrid parser in base class

A slightly larger refactor is to enhance `JwtTokenProvider` itself:

- Detect the `kid` / `alg` header in `extractAllClaims` and, when RSA is enabled, fetch the matching public key from `RSAJwtTokenProvider`.
- Keep a `JwtParserFactory` that can return the correct parser for each algorithm. This keeps helper methods centralized and avoids overriding each one.

This approach is useful if you expect additional algorithms or key rotation strategies in the future.

## Additional Considerations

- **Token Blacklist**: The blacklist service is Redis-backed (`src/main/java/com/focushive/identity/service/TokenBlacklistService.java`) and therefore requires Redis to be reachable before switching algorithms—otherwise refresh/validate will still fail for different reasons.
- **Configuration Consistency**: Once RSA validation works, verify that `jwt.issuer`, `spring.security.oauth2.authorizationserver.issuer`, and the values surfaced in `/.well-known/openid_configuration` stay in sync so that external clients can validate tokens correctly.
- **Instrumentation**: Add a dedicated metric/log when authentication fails because of signature validation to make diagnosing future key issues easier.

## Suggested Next Step

Implement Option A so that RSA signing and verification use the same key material when extracting claims. After deploying the fix, regenerate `/tmp/endpoint_results.json` to confirm authenticated endpoints pass end-to-end tests.
