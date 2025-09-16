package com.focushive.identity.controller;

import com.focushive.identity.security.UrlValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Security Test Controller for OWASP compliance testing.
 * Contains endpoints that demonstrate security features.
 * Only active in test/dev profiles.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Profile({"test", "dev", "local"})
public class SecurityTestController {

    private final UrlValidationUtil urlValidationUtil;

    /**
     * A10: Endpoint for testing SSRF prevention.
     * This would normally be a user avatar upload endpoint.
     */
    @PostMapping("/users/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        // A10: Validate URL to prevent SSRF attacks
        if (!urlValidationUtil.isUrlSafe(url)) {
            log.warn("SSRF attempt blocked for URL: {}", url);
            response.put("error", "Invalid or unsafe URL provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // In a real implementation, this would download and validate the image
        response.put("message", "Avatar URL validated successfully");
        response.put("url", url);
        return ResponseEntity.ok(response);
    }

    /**
     * A10: Endpoint for testing open redirect prevention.
     */
    @GetMapping("/v1/auth/redirect")
    public ResponseEntity<Map<String, Object>> handleRedirect(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();

        // A10: Validate redirect URL to prevent open redirect attacks
        if (!urlValidationUtil.isRedirectSafe(url)) {
            log.warn("Open redirect attempt blocked for URL: {}", url);
            response.put("error", "Invalid redirect URL provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // In a real implementation, this would perform the redirect
        response.put("message", "Redirect URL validated successfully");
        response.put("redirectUrl", url);
        return ResponseEntity.ok(response);
    }

}