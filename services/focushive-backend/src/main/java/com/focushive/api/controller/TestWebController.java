package com.focushive.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Test Web Controller for Security Configuration Testing
 * This controller provides web endpoints for testing security headers
 * Only enabled in test and development profiles
 */
@RestController
@RequestMapping("/web")
@RequiredArgsConstructor
@Slf4j
@Profile({"test", "dev", "security-test"})
public class TestWebController {

    /**
     * Test dashboard endpoint for web security headers
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> dashboard() {
        log.info("Test web dashboard endpoint called");

        Map<String, String> response = Map.of(
                "message", "Dashboard loaded (test mode)",
                "page", "dashboard",
                "user", "authenticated"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test embed widget endpoint (might need different frame options)
     */
    @GetMapping("/embed/widget")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> embedWidget() {
        log.info("Test embed widget endpoint called");

        Map<String, String> response = Map.of(
                "message", "Embed widget loaded (test mode)",
                "widget", "timer",
                "embeddable", "true"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Test public web content (no authentication required)
     */
    @GetMapping("/public/landing")
    public ResponseEntity<Map<String, String>> publicLanding() {
        log.info("Test public landing endpoint called");

        Map<String, String> response = Map.of(
                "message", "Public landing page (test mode)",
                "page", "landing",
                "public", "true"
        );

        return ResponseEntity.ok(response);
    }
}